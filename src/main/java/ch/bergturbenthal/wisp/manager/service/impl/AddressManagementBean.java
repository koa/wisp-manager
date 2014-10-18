package ch.bergturbenthal.wisp.manager.service.impl;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CompositeIterator;

import ch.bergturbenthal.wisp.manager.model.Antenna;
import ch.bergturbenthal.wisp.manager.model.AutoConnectionPort;
import ch.bergturbenthal.wisp.manager.model.CustomerConnection;
import ch.bergturbenthal.wisp.manager.model.DHCPSettings;
import ch.bergturbenthal.wisp.manager.model.GatewaySettings;
import ch.bergturbenthal.wisp.manager.model.GlobalDnsServer;
import ch.bergturbenthal.wisp.manager.model.IpAddress;
import ch.bergturbenthal.wisp.manager.model.IpIpv6Tunnel;
import ch.bergturbenthal.wisp.manager.model.IpNetwork;
import ch.bergturbenthal.wisp.manager.model.IpNetwork.AddressInNetwork;
import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.NetworkInterface;
import ch.bergturbenthal.wisp.manager.model.NetworkInterfaceRole;
import ch.bergturbenthal.wisp.manager.model.PortExpose;
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
import ch.bergturbenthal.wisp.manager.repository.IpIpv6TunnelRepository;
import ch.bergturbenthal.wisp.manager.repository.IpRangeRepository;
import ch.bergturbenthal.wisp.manager.repository.NetworkDeviceRepository;
import ch.bergturbenthal.wisp.manager.repository.PortExposeRepository;
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
			final IpRange ipRange = loadPojo(itemId);
			return ipRange == null || ipRange.getParentRange() == null;
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
	private final Set<AddressRangeType> AUTO_CLEANUP_RANGE_TYPE = new HashSet<AddressRangeType>(Arrays.asList(AddressRangeType.ASSIGNED, AddressRangeType.INTERMEDIATE));
	@Autowired
	private ConnectionRepository connectionRepository;
	@Autowired
	private DnsServerRepository dnsServerRepository;
	@Autowired
	private IpIpv6TunnelRepository ipIpv6TunnelRepository;
	@Autowired
	private IpRangeRepository ipRangeRepository;
	@Autowired
	private NetworkDeviceRepository networkDeviceRepository;
	@Autowired
	private PortExposeRepository portExposeRepository;
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

	@Override
	public void addPortExposition(final VLan vlan, final int port, final String address) {
		try {
			final IpAddress targetAddress = new IpAddress(InetAddress.getByName(address));
			if (targetAddress.getAddressType() == IpAddressType.V4) {
				// only one exposition per ipv4 port number
				for (final PortExpose replacedEntry : portExposeRepository.findV4ByPortNumber(port)) {
					final VLan enclosingVLan = replacedEntry.getVlan();
					if (enclosingVLan.equals(vlan) && targetAddress.equals(replacedEntry.getTargetAddress())) {
						continue;
					}
					removePortExpostion(replacedEntry);
				}
			}
			for (final PortExpose existingEntry : vlan.getExposion()) {
				if (targetAddress.equals(existingEntry.getTargetAddress()) && port == existingEntry.getPortNumber()) {
					// entry exists already
					return;
				}
			}
			final PortExpose expose = new PortExpose();
			expose.setPortNumber(port);
			expose.setTargetAddress(targetAddress);
			expose.setVlan(vlan);
			portExposeRepository.save(expose);
			vlan.getExposion().add(expose);
		} catch (final UnknownHostException e) {
			throw new IllegalArgumentException("Unknown IP address " + address, e);
		}

	}

	@Override
	public IpRange addRootRange(final IpNetwork reserveNetwork, final int reservationMask, final String comment) {
		if (reservationMask < reserveNetwork.getNetmask()) {
			throw new IllegalArgumentException("Error to create range for " + reserveNetwork
																					+ ": reservationMask ("
																					+ reservationMask
																					+ ") mask must be greater or equal than range mask ("
																					+ reserveNetwork.getNetmask()
																					+ ")");
		}
		final IpRange foundParentNetwork = findParentRange(reserveNetwork);
		if (foundParentNetwork != null) {
			throw new IllegalArgumentException("new range " + reserveNetwork + " overlaps with existsing " + foundParentNetwork);
		}
		final IpRange reservationRange = new IpRange(reserveNetwork, reservationMask, AddressRangeType.ROOT);
		reservationRange.setComment(comment);
		ipRangeRepository.save(reservationRange);
		return reservationRange;
	}

	private void appendRangeToMap(final Map<CustomerConnection, Collection<Runnable>> foundCustomerConnections,
																final CustomerConnection connection,
																final Runnable cleanupRunnable) {
		final Collection<Runnable> existingCollection = foundCustomerConnections.get(connection);
		if (existingCollection == null) {
			final ArrayList<Runnable> ranges = new ArrayList<Runnable>(2);
			ranges.add(cleanupRunnable);
			foundCustomerConnections.put(connection, ranges);
		} else {
			existingCollection.add(cleanupRunnable);
		}
	}

	private VLan appendVlan(final int vlanId, final RangePair parentAddresses) {
		final RangePair address = new RangePair();
		final VLan vLan = new VLan();
		vLan.setVlanId(Integer.valueOf(vlanId));
		if (parentAddresses.getV4Address() != null) {
			if (parentAddresses.getV4Address().getRangeMask() == 32) {
				address.setV4Address(parentAddresses.getV4Address());
			} else {
				address.setV4Address(reserveRange(parentAddresses.getV4Address(), AddressRangeType.ASSIGNED, 32, null));
			}
		}
		if (parentAddresses.getV6Address() != null) {
			address.setV6Address(reserveRange(parentAddresses.getV6Address(), AddressRangeType.ASSIGNED, 128, null));
		}
		vLan.setAddress(address);
		return vLan;
	}

	private void assignGateway(final NetworkInterface networkInterface, final GatewaySettings gatewaySettings) {
		networkInterface.setGatewaySettings(gatewaySettings);
		gatewaySettings.setNetworkInterface(networkInterface);
		networkInterface.setRole(NetworkInterfaceRole.GATEWAY);
	}

	private long calculateOffsetInParentRange(final IpRange v4Address, final String enteredAddress) throws UnknownHostException {
		final IpRange parentRange = v4Address.getParentRange();
		final long availableReservations = parentRange.getAvailableReservations();
		final InetAddress inetAddress = InetAddress.getByName(enteredAddress);
		final BigInteger numericAddress = IpAddress.inet2BigInteger(inetAddress);
		final BigInteger numericBaseAddress = parentRange.getRange().getAddress().getRawValue();
		final BigInteger offset = numericAddress.subtract(numericBaseAddress);
		final long offsetValue;
		if (offset.compareTo(BigInteger.ONE) < 1) {
			offsetValue = 1;
		} else if (offset.compareTo(BigInteger.valueOf(availableReservations)) > 0) {
			offsetValue = availableReservations - 1;
		} else {
			offsetValue = offset.longValue();
		}
		return offsetValue;
	}

	private void cleanupNetworkInterface(final NetworkInterface networkInterface) {
		if (networkInterface.getCustomerConnection() != null) {
			networkInterface.getCustomerConnection().setNetworkInterface(null);
			networkInterface.setCustomerConnection(null);
		}

		if (networkInterface.getAutoConnectionPort() != null) {
			networkInterface.getAutoConnectionPort().setNetworkInterface(null);
			networkInterface.setAutoConnectionPort(null);
		}
		if (networkInterface.getGatewaySettings() != null) {
			networkInterface.getGatewaySettings().setNetworkInterface(null);
			networkInterface.setGatewaySettings(null);
		}
		networkInterface.setRole(NetworkInterfaceRole.UNDEFINED);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Scheduled(fixedDelay = 60 * 1000, initialDelay = 500)
	public void cleanupOrphanRanges() {
		for (final IpRange range : ipRangeRepository.findByTypeIn(AUTO_CLEANUP_RANGE_TYPE)) {
			if (range.isOrphan()) {
				deleteIpRange(range);
			}
		}
	}

	private void cleanupRange(final IpRange range) {
		for (final Iterator<IpRange> iterator = range.getReservations().iterator(); iterator.hasNext();) {
			final IpRange subRange = iterator.next();
			subRange.setParentRange(null);
			deleteIpRange(subRange);
			iterator.remove();
		}
	}

	private void cleanupRangePair(final RangePair rangePair) {
		if (rangePair != null) {
			final IpRange v4Address = rangePair.getV4Address();
			if (v4Address != null) {
				cleanupRange(v4Address);
			}
			final IpRange v6Address = rangePair.getV6Address();
			if (v6Address != null) {
				cleanupRange(v6Address);
			}
		}
	}

	private void cleanVlans(final Set<VLan> vlans) {
		for (final Iterator<VLan> networkIter = vlans.iterator(); networkIter.hasNext();) {
			final VLan network = networkIter.next();
			removeRangePair(network.getAddress());
			vLanRepository.delete(network);
			networkIter.remove();
		}
	}

	private void clearIntermediateParent(final IpRange parentRange) {
		if (parentRange == null) {
			return;
		}
		if (parentRange.getType() != AddressRangeType.INTERMEDIATE) {
			return;
		}
		if (parentRange.getReservations().size() > 1) {
			return;
		}
		clearIntermediateParent(parentRange.getParentRange());
		deleteIpRange(parentRange);
	}

	@Override
	public CrudRepositoryContainer<IpRange, Long> createIpContainer() {
		return new IpRangeCrudContainer(ipRangeRepository, IpRange.class);
	}

	private void deleteIpRange(final IpRange range) {
		final IpRange parentRange = range.getParentRange();
		if (parentRange != null) {
			parentRange.getReservations().remove(range);
		}
		ipRangeRepository.delete(range);
	}

	@Override
	public String describeRangeUser(final IpRange ipRange) {
		final Collection<String> foundReferences = new ArrayList<String>();
		for (final VLan foundVlan : ipRange.getOwningVlans()) {
			final NetworkInterface networkInterface = foundVlan.getNetworkInterface();
			if (networkInterface != null) {
				final NetworkDevice networkDevice = networkInterface.getNetworkDevice();
				foundReferences.add("Network-Device: " + networkDevice.getTitle() + "; " + networkInterface.getInterfaceName() + ";" + foundVlan.getVlanId());
			}
			final CustomerConnection customerConnection = foundVlan.getCustomerConnection();
			if (customerConnection != null) {
				final Station station = customerConnection.getStation();
				foundReferences.add("Station Network: " + station.getName() + ";" + customerConnection.getName() + ";" + foundVlan.getVlanId());
			}
		}
		for (final Antenna foundAntenna : ipRange.getOwningAntennas()) {
			foundReferences.add("Antenna: " + foundAntenna.getTitle());
		}
		for (final Station stationLoopback : ipRange.getOwningStations()) {
			foundReferences.add("Station Loopback: " + stationLoopback.getName());
		}
		for (final AutoConnectionPort autoConnectionPort : ipRange.getOwningAutoConnectionPorts()) {
			foundReferences.add("Auto connection Port of " + autoConnectionPort.getStation().getName());
		}
		for (final GatewaySettings gatewaySettings : ipRange.getOwningGatewaySettings()) {
			foundReferences.add("Gateway Settings: " + gatewaySettings.getGatewayName());
		}
		for (final IpIpv6Tunnel tunnel : ipRange.getOwningTunnels()) {
			foundReferences.add("Tunnel: " + tunnel.getStartStation().getName() + "-" + tunnel.getEndStation().getName());
		}
		return StringUtils.join(foundReferences, ", ");
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

	private void fillLanIfNone(final Station station) {
		final Set<CustomerConnection> customerConnections;
		if (station.getCustomerConnections() == null) {
			customerConnections = new HashSet<CustomerConnection>();
		} else {
			customerConnections = station.getCustomerConnections();
		}
		for (final CustomerConnection customerConnection : customerConnections) {
			Set<VLan> customerNetworks;
			if (customerConnection.getOwnNetworks() == null) {
				customerNetworks = new HashSet<VLan>();
			} else {
				customerNetworks = customerConnection.getOwnNetworks();
			}
			if (customerNetworks.isEmpty()) {
				// add vlan 0 if none defined
				final VLan vLan = new VLan();
				vLan.setVlanId(0);
				vLan.setCustomerConnection(customerConnection);
				customerNetworks.add(vLan);
			}
			for (final VLan vLan : VLan.sortVLans(customerNetworks)) {
				final RangePair address;
				if (vLan.getAddress() == null) {
					address = new RangePair();
				} else {
					address = vLan.getAddress();
				}
				final boolean addDhcpSettings = address.getV4Address() == null;
				fillRangePair(address, AddressRangeType.USER, 25, 32, 64, 128, null);
				if (addDhcpSettings && vLan.getDhcpSettings() == null) {
					vLan.setDhcpSettings(new DHCPSettings());
				}
				if (vLan.getDhcpSettings() != null) {
					final DHCPSettings dhcpSettings = vLan.getDhcpSettings();
					if (dhcpSettings.getLeaseTime() == null) {
						dhcpSettings.setLeaseTime(Long.valueOf(TimeUnit.MINUTES.toMillis(30)));
					}
					if (dhcpSettings.getStartOffset() == null) {
						dhcpSettings.setStartOffset(Long.valueOf(20));
					}
					if (dhcpSettings.getEndOffset() == null) {
						dhcpSettings.setEndOffset(Long.valueOf(100));
					}
				}
				vLan.setAddress(address);
			}
			customerConnection.setOwnNetworks(customerNetworks);
		}
		station.setCustomerConnections(customerConnections);
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
		final Set<IpAddress> dnsServersOfDevice = ensureMutableSet(networkDevice.getDnsServers());
		final Collection<IpAddress> dnsServers;
		if (station.getDomain() != null && !station.getDomain().getDnsServers().isEmpty()) {
			dnsServers = station.getDomain().getDnsServers();
		} else {
			dnsServers = listGlobalDnsServers();
		}
		dnsServersOfDevice.retainAll(dnsServers);
		dnsServersOfDevice.addAll(dnsServers);
		networkDevice.setDnsServers(dnsServersOfDevice);
		// collect unassigned interfaces and connections at this station
		final Set<CustomerConnection> remainingCustomerConnections = new LinkedHashSet<CustomerConnection>(station.getCustomerConnections());
		// collect unassigned gateway settings
		final Set<GatewaySettings> remainingGatewaySettings = new LinkedHashSet<GatewaySettings>();
		for (final GatewaySettings gateway : station.getGatewaySettings()) {
			switch (gateway.getGatewayType()) {
			case LAN:
			case PPPOE:
				remainingGatewaySettings.add(gateway);
				break;
			default:
				break;
			}
		}
		// collect and adjust autoConnectionPorts
		final int fixeReservedPortCount = remainingGatewaySettings.size() + remainingCustomerConnections.size();
		final List<AutoConnectionPort> autoConnectionPorts = station.getAutoConnectionPorts();
		while (fixeReservedPortCount + autoConnectionPorts.size() < networkDevice.getInterfaces().size()) {
			final AutoConnectionPort autoConnectionPort = new AutoConnectionPort();
			autoConnectionPort.setPortAddress(new RangePair());
			fillRangePair(autoConnectionPort.getPortAddress(), AddressRangeType.CONNECTION, 29, 32, 64, 128, "");
			autoConnectionPort.setStation(station);
			autoConnectionPorts.add(autoConnectionPort);
		}
		while (!autoConnectionPorts.isEmpty() && fixeReservedPortCount + autoConnectionPorts.size() > networkDevice.getInterfaces().size()) {
			final AutoConnectionPort removedPort = autoConnectionPorts.remove(autoConnectionPorts.size() - 1);
			cleanupRangePair(removedPort.getPortAddress());
			removedPort.setStation(null);
		}
		final Set<AutoConnectionPort> remainingAutoConnectionPorts = new LinkedHashSet<>(autoConnectionPorts);
		final List<NetworkInterface> freeInterfaces = new ArrayList<>();
		final Set<NetworkInterface> userAssignedInterfaces = new LinkedHashSet<>();
		for (final NetworkInterface networkInterface : emptyIfNull(networkDevice.getInterfaces())) {
			if (networkInterface.getType() != NetworkInterfaceType.LAN) {
				continue;
			}
			final GatewaySettings gatewaySettings = networkInterface.getGatewaySettings();
			if (gatewaySettings != null && gatewaySettings.getStation() == station) {
				switch (gatewaySettings.getGatewayType()) {
				case LAN:
				case PPPOE:
					if (remainingGatewaySettings.remove(gatewaySettings)) {
						// keep gateway
						assignGateway(networkInterface, gatewaySettings);
						continue;
					}
					break;
				case HE:
					// remove -> HE needs no physical interface
					break;
				default:
				}
			}
			final CustomerConnection customerConnection = networkInterface.getCustomerConnection();
			if (customerConnection != null && customerConnection.getStation() == station) {
				remainingCustomerConnections.remove(customerConnection);
				continue;
			}
			final AutoConnectionPort autoConnectionPort = networkInterface.getAutoConnectionPort();
			if (autoConnectionPort != null && autoConnectionPort.getStation() == station) {
				remainingAutoConnectionPorts.remove(autoConnectionPort);
				continue;
			}
			// no connection -> free interface
			cleanupNetworkInterface(networkInterface);
			freeInterfaces.add(networkInterface);
		}

		// assign remaining interfaces to connections
		final Iterator<NetworkInterface> freeInterfacesIterator = freeInterfaces.iterator();
		final Iterator<GatewaySettings> remainingGatewayIterator = remainingGatewaySettings.iterator();
		while (freeInterfacesIterator.hasNext() && remainingGatewayIterator.hasNext()) {
			final NetworkInterface networkInterface = freeInterfacesIterator.next();
			final GatewaySettings gatewaySettings = remainingGatewayIterator.next();
			assignGateway(networkInterface, gatewaySettings);
		}

		final Iterator<CustomerConnection> remainingCustomerConnectionsIterator = remainingCustomerConnections.iterator();
		while (freeInterfacesIterator.hasNext() && remainingCustomerConnectionsIterator.hasNext()) {
			final NetworkInterface networkInterface = freeInterfacesIterator.next();
			// setup customer connections
			final CustomerConnection customerConnection = remainingCustomerConnectionsIterator.next();
			networkInterface.setCustomerConnection(customerConnection);
			networkInterface.setRole(NetworkInterfaceRole.NETWORK);
			customerConnection.setNetworkInterface(networkInterface);
			userAssignedInterfaces.add(networkInterface);
		}
		final Iterator<AutoConnectionPort> remainingAutoConnectionPortIterator = remainingAutoConnectionPorts.iterator();
		while (freeInterfacesIterator.hasNext() && remainingAutoConnectionPortIterator.hasNext()) {
			final NetworkInterface networkInterface = freeInterfacesIterator.next();
			final AutoConnectionPort autoConnectionPort = remainingAutoConnectionPortIterator.next();
			networkInterface.setAutoConnectionPort(autoConnectionPort);
			autoConnectionPort.setNetworkInterface(networkInterface);
			networkInterface.setRole(NetworkInterfaceRole.ROUTER_LINK);
		}

		fillTunnels(station, networkDevice);
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
		validatePortExpositions(station);
		fillNetworkDevice(station);
		return station;
	}

	private void fillTunnels(final Station station, final NetworkDevice networkDevice) {
		// setup available tunnel connections
		final HashSet<Station> foreignTunnelStations;
		if (station.isTunnelConnection()) {
			foreignTunnelStations = new HashSet<Station>();
			for (final Station tunnelStation : stationRepository.findAll()) {
				foreignTunnelStations.add(tunnelStation);
			}
		} else {
			foreignTunnelStations = new HashSet<Station>(stationRepository.findTunnelConnectionStations());
		}
		foreignTunnelStations.remove(station);
		final Map<Station, IpIpv6Tunnel> configuredTunnels = new HashMap<Station, IpIpv6Tunnel>();
		for (final IpIpv6Tunnel tunnel : station.getTunnelBegins()) {
			final Station partnerStation = tunnel.getEndStation();
			if (partnerStation == null) {
				ipIpv6TunnelRepository.delete(tunnel);
			} else {
				configuredTunnels.put(partnerStation, tunnel);
			}
		}
		for (final IpIpv6Tunnel tunnel : station.getTunnelEnds()) {
			final Station partnerStation = tunnel.getStartStation();
			if (partnerStation == null) {
				ipIpv6TunnelRepository.delete(tunnel);
			} else {
				configuredTunnels.put(partnerStation, tunnel);
			}
		}
		for (final Station tunnelPartnerStation : foreignTunnelStations) {
			final NetworkDevice partnerDevice = tunnelPartnerStation.getDevice();
			if (partnerDevice == null) {
				continue;
			}
			final IpIpv6Tunnel existingTunnel = configuredTunnels.remove(tunnelPartnerStation);
			if (existingTunnel == null) {
				final IpIpv6Tunnel tunnel = new IpIpv6Tunnel();
				tunnel.setStartStation(station);
				tunnel.setEndStation(tunnelPartnerStation);
				final IpRange tunnelAddressRange = findAndReserveAddressRange(AddressRangeType.TUNNEL, IpAddressType.V4, 30, 32, AddressRangeType.ASSIGNED, null);
				tunnel.setV4Address(tunnelAddressRange);
				station.getTunnelBegins().add(tunnel);
				tunnelPartnerStation.getTunnelEnds().add(tunnel);
				ipIpv6TunnelRepository.save(tunnel);
			}
		}
		for (final IpIpv6Tunnel tunnel : configuredTunnels.values()) {
			tunnel.getStartStation().getTunnelBegins().remove(tunnel);
			tunnel.getEndStation().getTunnelEnds().remove(tunnel);
			ipIpv6TunnelRepository.delete(tunnel);
		}
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

	private IpRange findParentRange(final IpNetwork reserveNetwork) {
		return findParentRange(reserveNetwork, findAllRootRanges());
	}

	private IpRange findParentRange(final IpNetwork reserveNetwork, final Collection<IpRange> collection) {
		for (final IpRange range : collection) {
			final IpNetwork checkNetwork = range.getRange();
			if (overlap(checkNetwork, reserveNetwork)) {
				final IpRange subRange = findParentRange(reserveNetwork, range.getReservations());
				if (subRange != null) {
					return subRange;
				}
				return range;
			}
		}
		return null;
	}

	@Override
	public String getDhcpEndAddress(final VLan vlan) {
		final IpRange v4Address = getV4AddressRange(vlan);
		if (v4Address == null) {
			return null;
		}
		final IpRange parentRange = v4Address.getParentRange();
		final long availableReservations = parentRange.getAvailableReservations();
		final DHCPSettings dhcpSettings = vlan.getDhcpSettings();
		if (dhcpSettings == null) {
			return null;
		}
		final Long endOffset = dhcpSettings.getEndOffset();
		if (endOffset == null) {
			return null;
		}
		if (endOffset.longValue() >= availableReservations) {
			dhcpSettings.setStartOffset(Long.valueOf(availableReservations - 1));
		}
		if (endOffset.longValue() < 1) {
			dhcpSettings.setEndOffset(Long.valueOf(1));
		}
		return parentRange.getRange().getAddress().getAddressOfNetwork(dhcpSettings.getEndOffset().longValue()).getHostAddress();

	}

	@Override
	public String getDhcpStartAddress(final VLan vlan) {
		final IpRange v4Address = getV4AddressRange(vlan);
		if (v4Address == null) {
			return null;
		}
		final IpRange parentRange = v4Address.getParentRange();
		final long availableReservations = parentRange.getAvailableReservations();
		final DHCPSettings dhcpSettings = vlan.getDhcpSettings();
		if (dhcpSettings == null) {
			return null;
		}
		final Long startOffset = dhcpSettings.getStartOffset();
		if (startOffset == null) {
			return null;
		}
		if (startOffset.longValue() >= availableReservations) {
			dhcpSettings.setStartOffset(Long.valueOf(availableReservations - 1));
		}
		if (startOffset.longValue() < 1) {
			dhcpSettings.setStartOffset(Long.valueOf(1));
		}
		return parentRange.getRange().getAddress().getAddressOfNetwork(dhcpSettings.getStartOffset().longValue()).getHostAddress();

	}

	private IpRange getV4AddressRange(final VLan vlan) {
		if (vlan == null) {
			return null;
		}
		final RangePair rangePair = vlan.getAddress();
		if (rangePair == null) {
			return null;
		}
		return rangePair.getV4Address();
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
				final IpRange ipV6GlobalReservationRange = addRootRange(new IpNetwork(new IpAddress(Inet6Address.getByName("2001:1620:bba::")), 48), 56, "Global v6 Range");
				reserveRange(ipV6GlobalReservationRange, AddressRangeType.USER, 64, "User Ranges");

				final IpRange ipV4ReservationRange = addRootRange(new IpNetwork(new IpAddress(Inet4Address.getByName("172.16.0.0")), 12), 16, "Internal v4 Range");
				final IpRange smallV4Ranges = reserveRange(ipV4ReservationRange, AddressRangeType.ADMINISTRATIVE, 24, "Some small Ranges");
				reserveRange(smallV4Ranges, AddressRangeType.LOOPBACK, 32, null);
				for (int i = 0; i < 3; i++) {
					reserveRange(smallV4Ranges, AddressRangeType.CONNECTION, 29, null);
				}
				reserveRange(smallV4Ranges, AddressRangeType.TUNNEL, 30, "IpIpv6-Tunnels");
				reserveRange(ipV4ReservationRange, AddressRangeType.USER, 24, null);
				final IpRange ipV6SiteLocalReservationRange = addRootRange(new IpNetwork(new IpAddress(Inet6Address.getByName("fd7e:907d:34ab::")), 48), 56, "Internal v6 Range");
				final IpRange singleRanges = reserveRange(ipV6SiteLocalReservationRange, AddressRangeType.ADMINISTRATIVE, 64, "Ranges for single addresses");
				reserveRange(singleRanges, AddressRangeType.LOOPBACK, 128, null);
				reserveRange(ipV6SiteLocalReservationRange, AddressRangeType.CONNECTION, 64, null);
				// reserveRange(ipV6SiteLocalReservationRange, AddressRangeType.USER, 64, null);
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
					compositeIterator.add(iteratorForRange(loopbackRange));
				}
				for (final IpRange range : ipRangeRepository.findMatchingRange(AddressRangeType.CONNECTION, IpAddressType.V4, 32)) {
					compositeIterator.add(iteratorForRange(range));
				}
				return compositeIterator;
			}

			private Iterator<InetAddress> iteratorForRange(final IpRange loopbackRange) {
				return new Iterator<InetAddress>() {
					final IpAddress address;
					long index = 1;
					final long lastAddressIndex;
					{
						final IpNetwork range = loopbackRange.getRange();
						lastAddressIndex = (1l << (32 - range.getNetmask())) - 1;
						address = range.getAddress();
					}

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
				};
			}
		};
	}

	private String makeInterfaceName(final CustomerConnection customerConnection) {
		if (customerConnection.getName() == null) {
			return "customer";
		}
		return "customer-" + customerConnection.getName();
	}

	private Map<Integer, VLan> orderNetworksByVlan(final Set<VLan> networks) {
		final Map<Integer, VLan> ret = new LinkedHashMap<Integer, VLan>();
		for (final VLan vLan : networks) {
			ret.put(vLan.getVlanId(), vLan);
		}
		return ret;
	}

	private boolean overlap(final IpNetwork checkNetwork, final IpNetwork reserveNetwork) {
		if (checkNetwork.containsAddress(reserveNetwork.getAddress())) {
			return true;
		}
		return reserveNetwork.containsAddress(checkNetwork.getAddress());
	}

	private void removeAddressIfOneVlan(final IpRange range) {
		final Collection<VLan> vlanOfRange = range.getOwningVlans();
		if (vlanOfRange.size() > 1) {
			return;
		}
		deleteIpRange(range);
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

	@Override
	public void removePortExpostion(final PortExpose exposition) {
		final VLan net = exposition.getVlan();
		if (net != null) {
			net.getExposion().remove(exposition);
		}
		portExposeRepository.delete(exposition);
	}

	@Override
	public void removeRange(final IpRange ipRange) {
		if (ipRange.isOrphan()) {
			deleteIpRange(ipRange);
		}
	}

	private void removeRangePair(final RangePair address) {
		if (address == null) {
			return;
		}
		final IpRange v4Address = address.getV4Address();
		if (v4Address != null) {
			removeAddressIfOneVlan(v4Address);
			address.setV4Address(null);
		}
		final IpRange v6Address = address.getV6Address();
		if (v6Address != null) {
			removeAddressIfOneVlan(v6Address);
			address.setV6Address(null);
		}
	}

	@Override
	public void removeRangeUsage(final IpRange range) {
		final IpAddressType addressType = range.getRange().getAddress().getAddressType();

		for (final Antenna antenna : range.getOwningAntennas()) {
			antenna.getAddresses().setIpAddress(null, addressType);
		}
		range.getOwningAntennas().clear();

		for (final AutoConnectionPort autoConnectionPort : range.getOwningAutoConnectionPorts()) {
			autoConnectionPort.getPortAddress().setIpAddress(null, addressType);
		}
		range.getOwningAutoConnectionPorts().clear();

		for (final GatewaySettings gatewaySettings : range.getOwningGatewaySettings()) {
			gatewaySettings.getManagementAddress().setIpAddress(null, addressType);
		}
		range.getOwningGatewaySettings().clear();

		for (final Station station : range.getOwningStations()) {
			station.getLoopback().setIpAddress(null, addressType);
		}
		range.getOwningStations().clear();

		for (final VLan vLan : range.getOwningVlans()) {
			vLan.getAddress().setIpAddress(null, addressType);
		}
		range.getOwningVlans().clear();
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

	@Override
	public boolean setAddressManually(final RangePair addressPair, final String address, final IpAddressType addressType) {
		final AddressInNetwork addressInNetwork = IpNetwork.resolveAddressInNetwork(address);
		if (addressInNetwork == null) {
			addressPair.setIpAddress(null, addressType);
			// if (offsetPair != null) {
			// offsetPair.setExpectedOffset(null, addressType);
			// }
			return true;
		}
		final IpRange reservationBefore = addressPair.getIpAddress(addressType);
		if (reservationBefore != null) {
			clearIntermediateParent(reservationBefore.getParentRange());
			deleteIpRange(reservationBefore);
			addressPair.setIpAddress(null, addressType);
		}
		final IpNetwork enteredNetwork = addressInNetwork.getNetwork();
		final IpAddress enteredAddress = addressInNetwork.getAddress();
		if (enteredAddress.getAddressType() != addressType) {
			log.info("Wrong address type " + enteredAddress.getAddressType() + " of " + address + " expected " + addressType);
			return false;
		}
		final IpRange foundParentRange = findParentRange(enteredNetwork);
		final IpRange reservedIntermediateRange;
		final int addressMask = enteredNetwork.getNetmask();
		final int singleAddressMask = addressType.getBitCount();
		if (foundParentRange == null) {
			// reserve special range
			if (addressMask > singleAddressMask - 2) {
				log.info("Cannot create reservation for " + address);
				return false;
			}
			final IpRange rootRange = addRootRange(enteredNetwork, addressMask, "");
			reservedIntermediateRange = reserveRange(rootRange, AddressRangeType.INTERMEDIATE, singleAddressMask, "");
		} else {
			final int rangeMask = foundParentRange.getRangeMask();
			final IpNetwork ipNetwork = new IpNetwork(enteredAddress, rangeMask);
			for (final IpRange checkRange : foundParentRange.getReservations()) {
				if (ipNetwork.getAddress().getRawValue().equals(checkRange.getRange().getAddress().getRawValue())) {
					log.info("Address-Range " + ipNetwork + " is already reserved");
					return false;
				}
			}
			final boolean intermediateRangeNeeded = rangeMask < addressMask;
			final IpRange immediateReservationRange = new IpRange(ipNetwork, intermediateRangeNeeded ? addressMask : singleAddressMask, AddressRangeType.INTERMEDIATE);
			immediateReservationRange.setParentRange(foundParentRange);
			foundParentRange.getReservations().add(immediateReservationRange);
			if (intermediateRangeNeeded) {
				final IpNetwork ipSubNetwork = new IpNetwork(enteredAddress, addressMask);
				reservedIntermediateRange = new IpRange(ipSubNetwork, singleAddressMask, AddressRangeType.INTERMEDIATE);
				reservedIntermediateRange.setParentRange(immediateReservationRange);
				immediateReservationRange.getReservations().add(reservedIntermediateRange);
			} else {
				reservedIntermediateRange = immediateReservationRange;
			}
			ipRangeRepository.save(immediateReservationRange);
		}
		final IpNetwork reservationNetwork = new IpNetwork(enteredAddress, singleAddressMask);
		final IpRange reservedRange = new IpRange(reservationNetwork, singleAddressMask, AddressRangeType.ASSIGNED);
		reservedRange.setParentRange(reservedIntermediateRange);
		reservedIntermediateRange.getReservations().add(reservedRange);
		ipRangeRepository.save(reservedRange);
		addressPair.setIpAddress(reservedRange, addressType);
		return true;
	}

	@Override
	public boolean setDhcpEndAddress(final VLan vlan, final String endAddress) {
		try {
			final IpRange v4Address = getV4AddressRange(vlan);
			if (v4Address == null) {
				return false;
			}
			final long offsetValue = calculateOffsetInParentRange(v4Address, endAddress);
			if (vlan.getDhcpSettings() == null) {
				vlan.setDhcpSettings(new DHCPSettings());
			}
			vlan.getDhcpSettings().setEndOffset(Long.valueOf(offsetValue));
			return true;
		} catch (final UnknownHostException e) {
			log.info("Unknown IP Address", e);
			return false;
		}
	}

	@Override
	public boolean setDhcpStartAddress(final VLan vlan, final String startAddress) {
		try {
			final IpRange v4Address = getV4AddressRange(vlan);
			if (v4Address == null) {
				return false;
			}
			final long offsetValue = calculateOffsetInParentRange(v4Address, startAddress);
			if (vlan.getDhcpSettings() == null) {
				vlan.setDhcpSettings(new DHCPSettings());
			}
			vlan.getDhcpSettings().setStartOffset(Long.valueOf(offsetValue));
			return true;
		} catch (final UnknownHostException e) {
			log.info("Unknown IP Address", e);
			return false;
		}
	}

	private void validatePortExpositions(final Station station) {
		final List<PortExpose> invalidExpositions = new ArrayList<PortExpose>();
		for (final CustomerConnection cc : station.getCustomerConnections()) {
			for (final VLan net : cc.getOwnNetworks()) {
				for (final PortExpose exposition : net.getExposion()) {
					final IpAddress targetAddress = exposition.getTargetAddress();
					final IpRange ipAddress = net.getAddress().getIpAddress(targetAddress.getAddressType());
					final boolean addressOk;
					if (targetAddress.getAddressType().getBitCount() == ipAddress.getRange().getNetmask()) {
						addressOk = ipAddress.getParentRange().getRange().containsAddress(targetAddress);
					} else {
						addressOk = ipAddress.getRange().containsAddress(targetAddress);
					}
					if (!addressOk) {
						invalidExpositions.add(exposition);
					}
				}
			}
		}
		for (final PortExpose exposition : invalidExpositions) {
			removePortExpostion(exposition);
		}
	}
}
