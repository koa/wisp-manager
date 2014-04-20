package ch.bergturbenthal.wisp.manager.service.impl;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CompositeIterator;

import ch.bergturbenthal.wisp.manager.model.Antenna;
import ch.bergturbenthal.wisp.manager.model.Bridge;
import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.GlobalDnsServer;
import ch.bergturbenthal.wisp.manager.model.IpAddress;
import ch.bergturbenthal.wisp.manager.model.IpNetwork;
import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.NetworkInterface;
import ch.bergturbenthal.wisp.manager.model.NetworkInterfaceRole;
import ch.bergturbenthal.wisp.manager.model.RangePair;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.VLan;
import ch.bergturbenthal.wisp.manager.model.address.AddressRangeType;
import ch.bergturbenthal.wisp.manager.model.address.IpAddressType;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkInterfaceType;
import ch.bergturbenthal.wisp.manager.repository.AntennaRepository;
import ch.bergturbenthal.wisp.manager.repository.ConnectionRepository;
import ch.bergturbenthal.wisp.manager.repository.DnsServerRepository;
import ch.bergturbenthal.wisp.manager.repository.IpRangeRepository;
import ch.bergturbenthal.wisp.manager.repository.NetworkDeviceRepository;
import ch.bergturbenthal.wisp.manager.repository.StationRepository;
import ch.bergturbenthal.wisp.manager.repository.VLanRepository;
import ch.bergturbenthal.wisp.manager.service.AddressManagementService;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;

import com.vaadin.data.Container;

@Slf4j
@Component
@Transactional
public class AddressManagementBean implements AddressManagementService {

	private static class IpRangeCrudContainer extends CrudRepositoryContainer<IpRange, Long> implements Container.Hierarchical {
		private final IpRangeRepository repository;

		private IpRangeCrudContainer(final IpRangeRepository repository, final Class<IpRange> entityType) {
			super(repository, entityType);
			this.repository = repository;
		}

		@Override
		public boolean areChildrenAllowed(final Object itemId) {
			return true;
		}

		@Override
		public Collection<?> getChildren(final Object itemId) {
			final IpRange ipRange = loadPojo(itemId);
			final ArrayList<Long> childrenIds = new ArrayList<Long>();
			for (final IpRange reservationRange : ipRange.getReservations()) {
				childrenIds.add(reservationRange.getId());
			}
			return childrenIds;
		}

		@Override
		public Object getParent(final Object itemId) {
			final IpRange parentRange = loadPojo(itemId).getParentRange();
			if (parentRange == null) {
				return null;
			}
			return parentRange.getId();
		}

		@Override
		public boolean hasChildren(final Object itemId) {
			final Collection<IpRange> reservations = loadPojo(itemId).getReservations();
			return reservations != null && !reservations.isEmpty();
		}

		@Override
		protected Long idFromValue(final IpRange entry) {
			return entry.getId();
		}

		@Override
		public boolean isRoot(final Object itemId) {
			return loadPojo(itemId).getParentRange() == null;
		}

		private IpRange loadPojo(final Object itemId) {
			return repository.findOne((Long) itemId);
		}

		@Override
		public Collection<?> rootItemIds() {
			final ArrayList<Long> ret = new ArrayList<Long>();
			for (final IpRange range : repository.findAllRootRanges()) {
				ret.add(range.getId());
			}
			return ret;
		}

		@Override
		public boolean setChildrenAllowed(final Object itemId, final boolean areChildrenAllowed) throws UnsupportedOperationException {
			return false;
		}

		@Override
		public boolean setParent(final Object itemId, final Object newParentId) throws UnsupportedOperationException {
			return false;
		}
	}

	@Autowired
	private AntennaRepository antennaRepository;
	@Autowired
	private ConnectionRepository connectionRepository;
	@Autowired
	private DnsServerRepository dnsServerRepository;
	@Autowired
	private IpRangeRepository ipRangeRepository;
	@Autowired
	private NetworkDeviceRepository networkDeviceRepository;
	@Autowired
	private StationRepository stationRepository;
	@Autowired
	private VLanRepository vLanRepository;

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.bergturbenthal.wisp.manager.service.impl.AddressManagementService#addGlobalDns(ch.bergturbenthal.wisp.manager.model.IpAddress)
	 */
	@Override
	public void addGlobalDns(final IpAddress address) {
		dnsServerRepository.save(new GlobalDnsServer(address));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.bergturbenthal.wisp.manager.service.impl.AddressManagementService#addRootRange(java.net.InetAddress, int, int, java.lang.String)
	 */
	@Override
	public IpRange addRootRange(final InetAddress rangeAddress, final int rangeMask, final int reservationMask, final String comment) {
		if (reservationMask < rangeMask) {
			throw new IllegalArgumentException("Error to create range for " + rangeAddress
																					+ "/"
																					+ rangeMask
																					+ ": reservationMask ("
																					+ reservationMask
																					+ ") mask must be greater or equal than range mask ("
																					+ rangeMask
																					+ ")");
		}
		final IpNetwork reserveNetwork = new IpNetwork(new IpAddress(rangeAddress), rangeMask);
		for (final IpRange range : findAllRootRanges()) {
			final IpNetwork checkNetwork = range.getRange();
			if (overlap(checkNetwork, reserveNetwork)) {
				throw new IllegalArgumentException("new range " + reserveNetwork + " overlaps with existsing " + checkNetwork);
			}
		}
		final IpRange reservationRange = new IpRange(reserveNetwork, reservationMask, AddressRangeType.ROOT);
		reservationRange.setComment(comment);
		ipRangeRepository.save(reservationRange);
		return reservationRange;
	}

	private VLan appendVlan(final int vlanId, final RangePair parentAddresses) {
		final RangePair address = new RangePair();
		final VLan vLan = new VLan();
		vLan.setVlanId(Integer.valueOf(vlanId));
		if (parentAddresses.getV4Address() != null) {
			address.setV4Address(reserveRange(parentAddresses.getV4Address(), AddressRangeType.ASSIGNED, 32, null));
		}
		if (parentAddresses.getV6Address() != null) {
			address.setV6Address(reserveRange(parentAddresses.getV6Address(), AddressRangeType.ASSIGNED, 128, null));
		}
		vLan.setAddress(address);
		return vLan;
	}

	@Override
	public CrudRepositoryContainer<IpRange, Long> createIpContainer() {
		return new IpRangeCrudContainer(ipRangeRepository, IpRange.class);
	}

	@Override
	public String describeRangeUser(final IpRange ipRange) {
		final VLan foundVlan = vLanRepository.findVlanByRange(ipRange);
		if (foundVlan != null) {
			final NetworkInterface networkInterface = foundVlan.getNetworkInterface();
			if (networkInterface != null) {
				final NetworkDevice networkDevice = networkInterface.getNetworkDevice();
				return "Network-Device: " + networkDevice.getTitle() + "; " + networkInterface.getInterfaceName() + ";" + foundVlan.getVlanId();
			}
			final Station station = foundVlan.getStation();
			return "Station Network: " + station.getName() + ";" + foundVlan.getVlanId();
		}
		final Antenna foundAntenna = antennaRepository.findAntennaForRange(ipRange);
		if (foundAntenna != null) {
			return "Antenna: " + foundAntenna.getTitle();
		}
		final Connection foundConnection = connectionRepository.findConnectionForRange(ipRange);
		if (foundConnection != null) {
			return "Connection: " + foundConnection.getTitle();
		}
		final NetworkDevice foundNetworkDevice = networkDeviceRepository.findDeviceForRange(ipRange);
		if (foundNetworkDevice != null) {
			return "Network-Device: " + foundNetworkDevice.getTitle();
		}
		final Station stationLoopback = stationRepository.findStationLoopbackForRange(ipRange);
		if (stationLoopback != null) {
			return "Station Loopback: " + stationLoopback.getName();
		}
		final Station stationNetwork = stationRepository.findStationNetworkForRange(ipRange);
		if (stationNetwork != null) {
			return "Station Network: " + stationNetwork.getName();
		}
		return null;
	}

	private <T> List<T> emptyIfNull(final List<T> collection) {
		if (collection == null) {
			return java.util.Collections.emptyList();
		}
		return collection;
	}

	private <T> Set<T> ensureMutableSet(final Set<T> set) {
		if (set == null) {
			return new HashSet<>();
		}
		return set;
	}

	private void fillAntenna(final Antenna antenna, final RangePair parentAddresses) {
		if (antenna.getAddresses() == null) {
			antenna.setAddresses(new RangePair());
		}
		final RangePair rangePair = antenna.getAddresses();
		if (rangePair.getV4Address() == null) {
			rangePair.setV4Address(reserveRange(parentAddresses.getV4Address(), AddressRangeType.ASSIGNED, 32, null));
		}
		if (rangePair.getV6Address() == null) {
			rangePair.setV6Address(reserveRange(parentAddresses.getV6Address(), AddressRangeType.ASSIGNED, 128, null));
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.bergturbenthal.wisp.manager.service.impl.AddressManagementService#fillConnection(ch.bergturbenthal.wisp.manager.model.Connection)
	 */
	@Override
	public Connection fillConnection(final Connection connection) {
		final RangePair addresses;
		if (connection.getAddresses() == null) {
			addresses = new RangePair();
		} else {
			addresses = connection.getAddresses();
		}
		fillRangePair(addresses, AddressRangeType.CONNECTION, 29, 32, 64, 128, "Connection " + connection.getId());
		connection.setAddresses(addresses);
		for (final Bridge bridge : connection.getBridges()) {
			fillAntenna(bridge.getApAntenna(), addresses);
			fillAntenna(bridge.getClientAntenna(), addresses);
		}
		return connection;
	}

	private void fillLanIfNone(final Station station) {
		Set<VLan> ownNetworks;
		if (station.getOwnNetworks() == null) {
			ownNetworks = new HashSet<>();
		} else {
			ownNetworks = station.getOwnNetworks();
		}
		if (ownNetworks.isEmpty()) {
			final VLan vLan = new VLan();
			vLan.setVlanId(0);
			vLan.setStation(station);
			ownNetworks.add(vLan);
		}
		for (final VLan vLan : ownNetworks) {
			final RangePair address;
			if (vLan.getAddress() == null) {
				address = new RangePair();
			} else {
				address = vLan.getAddress();
			}
			fillRangePair(address, AddressRangeType.USER, 25, 32, 64, 128, null);
			vLan.setAddress(address);
		}
		station.setOwnNetworks(ownNetworks);
	}

	private void fillLoopbackAddress(final Station station) {
		final RangePair loopback;
		if (station.getLoopback() == null) {
			loopback = new RangePair();
		} else {
			loopback = station.getLoopback();
		}
		fillRangePair(loopback, AddressRangeType.LOOPBACK, 32, 32, 128, 128, "Station " + station.getName());
		station.setLoopback(loopback);
	}

	private void fillNetworkDevice(final Station station) {
		final NetworkDevice networkDevice = station.getDevice();
		if (networkDevice == null) {
			return;
		}
		final Collection<IpAddress> dnsServers = listGlobalDnsServers();
		final Set<IpAddress> dnsServersOfDevice = ensureMutableSet(networkDevice.getDnsServers());
		dnsServersOfDevice.retainAll(dnsServers);
		dnsServersOfDevice.addAll(dnsServers);
		networkDevice.setDnsServers(dnsServersOfDevice);
		// collect unassigned interfaces and connections at this station
		final Set<Connection> remainingConnections = new HashSet<>(emptyIfNull(station.getConnections()));
		final List<NetworkInterface> freeInterfaces = new ArrayList<>();
		final Set<NetworkInterface> userAssignedInterfaces = new HashSet<>();
		for (final NetworkInterface networkInterface : emptyIfNull(networkDevice.getInterfaces())) {
			if (networkInterface.getType() != NetworkInterfaceType.LAN) {
				continue;
			}
			final Set<VLan> networks = networkInterface.getNetworks();
			if (networks == null || networks.isEmpty()) {
				// no connection -> free interface
				freeInterfaces.add(networkInterface);
				networkInterface.setRole(NetworkInterfaceRole.UNDEFINED);
			} else {
				// find connections and stations for this interface
				final Set<Connection> foundConnections = new HashSet<>();
				final Set<Station> foundStations = new HashSet<>();
				for (final VLan vLan : networks) {
					final RangePair connectionAddress = vLan.getAddress();
					if (connectionAddress == null) {
						continue;
					}
					if (connectionAddress.getV4Address() != null) {
						foundStations.add(findStationForRange(connectionAddress.getV4Address().getParentRange()));
						foundConnections.add(findConnectionForRange(connectionAddress.getV4Address().getParentRange()));
					}
					if (connectionAddress.getV6Address() != null) {
						foundStations.add(findStationForRange(connectionAddress.getV6Address().getParentRange()));
						foundConnections.add(findConnectionForRange(connectionAddress.getV6Address().getParentRange()));
					}
				}
				// remove not found entries
				foundConnections.remove(null);
				foundStations.remove(null);
				if (foundStations.isEmpty() && foundConnections.isEmpty()) {
					// unassigned interface
					freeInterfaces.add(networkInterface);
					networkInterface.setRole(NetworkInterfaceRole.UNDEFINED);
					continue;
				}
				if (foundStations.isEmpty() && foundConnections.size() == 1) {
					// correct connected interface
					final Connection connection = foundConnections.iterator().next();
					updateInterfaceTitle(networkInterface, connection);
					remainingConnections.remove(connection);
					networkInterface.setRole(NetworkInterfaceRole.ROUTER_LINK);
					continue;
				}
				if (foundStations.size() == 1 && foundConnections.isEmpty()) {
					final Station foundStation = foundStations.iterator().next();
					if (foundStation.equals(station)) {
						// locally connected interface
						networkInterface.setInterfaceName("home");
						networkInterface.setRole(NetworkInterfaceRole.NETWORK);
						userAssignedInterfaces.add(networkInterface);
						continue;
					}
				}
				// every other case is inconsistent and will be cleaned
				log.warn("Inconsisten connection at interface " + networkInterface + " cleaning");
				networkInterface.getNetworks().clear();
				freeInterfaces.add(networkInterface);
				networkInterface.setRole(NetworkInterfaceRole.UNDEFINED);
			}
		}
		// assign remaining interfaces to connections
		final Iterator<NetworkInterface> freeInterfacesIterator = freeInterfaces.iterator();
		final Iterator<Connection> unassignedConnectionsIterator = remainingConnections.iterator();
		while (freeInterfacesIterator.hasNext() && userAssignedInterfaces.isEmpty()) {
			final NetworkInterface networkInterface = freeInterfacesIterator.next();
			if (networkInterface.getType() != NetworkInterfaceType.LAN) {
				// only lan interfaces
				continue;
			}
			// first interface is always for user connection
			final Set<VLan> ownNetworks = station.getOwnNetworks();
			if (ownNetworks != null && !ownNetworks.isEmpty()) {
				final Set<VLan> networks = ensureMutableSet(networkInterface.getNetworks());
				networks.clear();
				for (final VLan vlan : ownNetworks) {
					final VLan ifaceVlan = appendVlan(vlan.getVlanId(), vlan.getAddress());
					networks.add(ifaceVlan);
					ifaceVlan.setNetworkInterface(networkInterface);
				}
			}
			networkInterface.setInterfaceName("home");
			networkInterface.setRole(NetworkInterfaceRole.NETWORK);
			userAssignedInterfaces.add(networkInterface);
		}
		while (freeInterfacesIterator.hasNext() && unassignedConnectionsIterator.hasNext()) {
			final NetworkInterface networkInterface = freeInterfacesIterator.next();
			if (networkInterface.getType() != NetworkInterfaceType.LAN) {
				// only lan interfaces
				continue;
			}
			// take connection from list
			final Connection connection = unassignedConnectionsIterator.next();
			fillConnection(connection);
			final Set<VLan> networks = ensureMutableSet(networkInterface.getNetworks());
			networks.clear();
			final VLan vLan = appendVlan(0, connection.getAddresses());

			vLan.setNetworkInterface(networkInterface);
			networks.add(vLan);
			networkInterface.setNetworks(networks);
			networkInterface.setRole(NetworkInterfaceRole.ROUTER_LINK);
			updateInterfaceTitle(networkInterface, connection);
		}
	}

	private void fillRangePair(	final RangePair pair,
															final AddressRangeType rangeType,
															final int v4Netmask,
															final int v4NextMask,
															final int v6Netmask,
															final int v6NextMask,
															final String comment) {
		if (pair.getV4Address() == null) {
			pair.setV4Address(findAndReserveAddressRange(rangeType, IpAddressType.V4, v4Netmask, v4NextMask, AddressRangeType.ASSIGNED, comment));
		}
		if (pair.getV6Address() == null) {
			pair.setV6Address(findAndReserveAddressRange(rangeType, IpAddressType.V6, v6Netmask, v6NextMask, AddressRangeType.ASSIGNED, comment));
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.bergturbenthal.wisp.manager.service.impl.AddressManagementService#fillStation(ch.bergturbenthal.wisp.manager.model.Station)
	 */
	@Override
	public Station fillStation(final Station station) {
		fillLoopbackAddress(station);
		fillLanIfNone(station);
		fillNetworkDevice(station);
		return station;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.bergturbenthal.wisp.manager.service.impl.AddressManagementService#findAllRootRanges()
	 */
	@Override
	public List<IpRange> findAllRootRanges() {
		return ipRangeRepository.findAllRootRanges();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * ch.bergturbenthal.wisp.manager.service.impl.AddressManagementService#findAndReserveAddressRange(ch.bergturbenthal.wisp.manager.model.address.
	 * AddressRangeType, ch.bergturbenthal.wisp.manager.model.address.IpAddressType, int, int,
	 * ch.bergturbenthal.wisp.manager.model.address.AddressRangeType, java.lang.String)
	 */
	@Override
	public IpRange findAndReserveAddressRange(final AddressRangeType rangeType,
																						final IpAddressType addressType,
																						final int maxNetSize,
																						final int nextDistributionSize,
																						final AddressRangeType typeOfReservation,
																						final String comment) {
		final IpRange parentRange = findMatchingRange(rangeType, addressType, maxNetSize);
		if (parentRange == null) {
			return null;
		}
		return reserveRange(parentRange, typeOfReservation == null ? AddressRangeType.ASSIGNED : typeOfReservation, nextDistributionSize, comment);
	}

	private Connection findConnectionForRange(final IpRange connectionRange) {
		return connectionRepository.findConnectionForRange(connectionRange);
	}

	private IpRange findMatchingRange(final AddressRangeType rangeType, final IpAddressType addressType, final int maxNetSize) {
		for (final IpRange range : ipRangeRepository.findMatchingRange(rangeType, addressType, maxNetSize)) {
			if (range.getAvailableReservations() <= range.getReservations().size()) {
				// range full
				continue;
			}
			return range;
		}
		// no matching range found
		return null;
	}

	private Station findStationForRange(final IpRange range) {
		return stationRepository.findStationLoopbackForRange(range);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.bergturbenthal.wisp.manager.service.impl.AddressManagementService#initAddressRanges()
	 */
	@Override
	public void initAddressRanges() {
		try {
			final List<IpRange> resultList = findAllRootRanges();
			// log.info("Ranges: " + resultList);
			if (resultList.isEmpty()) {

				final IpRange ipV4ReservationRange = addRootRange(Inet4Address.getByName("172.16.0.0"), 12, 16, "Internal v4 Range");
				final IpRange smallV4Ranges = reserveRange(ipV4ReservationRange, AddressRangeType.ADMINISTRATIVE, 24, "Some small Ranges");
				reserveRange(smallV4Ranges, AddressRangeType.LOOPBACK, 32, null);
				reserveRange(smallV4Ranges, AddressRangeType.CONNECTION, 29, null);
				reserveRange(ipV4ReservationRange, AddressRangeType.USER, 24, null);
				final IpRange ipV6ReservationRange = addRootRange(Inet6Address.getByName("fd7e:907d:34ab::"), 48, 56, "Internal v6 Range");
				final IpRange singleRanges = reserveRange(ipV6ReservationRange, AddressRangeType.ADMINISTRATIVE, 64, "Ranges for single addresses");
				reserveRange(singleRanges, AddressRangeType.LOOPBACK, 128, null);
				reserveRange(ipV6ReservationRange, AddressRangeType.CONNECTION, 64, null);
				reserveRange(ipV6ReservationRange, AddressRangeType.USER, 64, null);
			}
			if (listGlobalDnsServers().isEmpty()) {
				addGlobalDns(new IpAddress(InetAddress.getByName("8.8.8.8")));
				addGlobalDns(new IpAddress(InetAddress.getByName("2001:4860:4860::8888")));
			}
		} catch (final UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.bergturbenthal.wisp.manager.service.impl.AddressManagementService#listGlobalDnsServers()
	 */
	@Override
	public Collection<IpAddress> listGlobalDnsServers() {
		final Collection<IpAddress> ret = new ArrayList<>();
		for (final GlobalDnsServer server : dnsServerRepository.findAll()) {
			ret.add(server.getAddress());
		}
		return ret;
	}

	@Override
	public Iterable<InetAddress> listPossibleNetworkDevices() {
		return new Iterable<InetAddress>() {

			@Override
			public Iterator<InetAddress> iterator() {
				final CompositeIterator<InetAddress> compositeIterator = new CompositeIterator<InetAddress>();
				final Collection<InetAddress> defaultAddresses = new HashSet<InetAddress>();
				for (final NetworkDeviceModel model : NetworkDeviceModel.values()) {
					defaultAddresses.add(model.getFactoryDefaultAddress());
				}
				compositeIterator.add(defaultAddresses.iterator());
				for (final IpRange loopbackRange : ipRangeRepository.findV4LoopbackRanges()) {
					final IpNetwork range = loopbackRange.getRange();
					final long lastAddressIndex = (1l << (32 - range.getNetmask())) - 1;
					final IpAddress address = range.getAddress();
					compositeIterator.add(new Iterator<InetAddress>() {
						long index = 1;

						@Override
						public boolean hasNext() {
							return index < lastAddressIndex;
						}

						@Override
						public InetAddress next() {
							return address.getAddressOfNetwork(index++);
						}

						@Override
						public void remove() {
							throw new UnsupportedOperationException("cannot remove a possible address");
						}
					});
				}
				return compositeIterator;
			}
		};
	}

	private boolean overlap(final IpNetwork checkNetwork, final IpNetwork reserveNetwork) {
		if (checkNetwork.containsAddress(reserveNetwork.getAddress())) {
			return true;
		}
		return reserveNetwork.containsAddress(checkNetwork.getAddress());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.bergturbenthal.wisp.manager.service.impl.AddressManagementService#removeGlobalDns(ch.bergturbenthal.wisp.manager.model.IpAddress)
	 */
	@Override
	public void removeGlobalDns(final IpAddress address) {
		dnsServerRepository.delete(address);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.bergturbenthal.wisp.manager.service.impl.AddressManagementService#reserveRange(ch.bergturbenthal.wisp.manager.model.IpRange,
	 * ch.bergturbenthal.wisp.manager.model.address.AddressRangeType, int, java.lang.String)
	 */
	@Override
	public IpRange reserveRange(final IpRange parentRange, final AddressRangeType type, final int mask, final String comment) {
		if (mask < parentRange.getRangeMask()) {
			throw new IllegalArgumentException("To big range: " + mask + " parent allowes " + parentRange.getRangeMask());
		}
		final boolean isV4 = parentRange.getRange().getAddress().getAddressType() == IpAddressType.V4;
		final BigInteger parentRangeStartAddress = parentRange.getRange().getAddress().getRawValue();
		final BigInteger rangeSize = BigInteger.valueOf(1).shiftLeft((isV4 ? 32 : 128) - parentRange.getRangeMask());
		// for single v4-address -> skip first and last address
		final boolean isV4SingleAddress = parentRange.getRangeMask() == 32 && isV4;
		final long availableReservations = isV4SingleAddress ? parentRange.getAvailableReservations() - 1 : parentRange.getAvailableReservations();
		nextReservation:
		for (int i = isV4SingleAddress ? 1 : 0; i < availableReservations; i++) {
			final BigInteger candidateAddress = parentRangeStartAddress.add(rangeSize.multiply(BigInteger.valueOf(i)));
			final Collection<IpRange> reservations = parentRange.getReservations();
			for (final IpRange reservationRange : reservations) {
				if (reservationRange.getRange().getAddress().getRawValue().equals(candidateAddress)) {
					continue nextReservation;
				}
			}
			// reservation is free
			final IpRange newRange = new IpRange(new IpNetwork(new IpAddress(candidateAddress), parentRange.getRangeMask()), mask, type);
			newRange.setParentRange(parentRange);
			parentRange.getReservations().add(newRange);
			newRange.setComment(comment);
			log.info("Reserved: " + newRange);
			return ipRangeRepository.save(newRange);
		}
		// no free reservation found in range
		return null;
	}

	private void updateInterfaceTitle(final NetworkInterface networkInterface, final Connection connection) {
		networkInterface.setInterfaceName("connection: " + connection.getTitle());
	}
}
