package ch.bergturbenthal.wisp.manager.service;

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

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import lombok.extern.slf4j.Slf4j;
import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.IpAddress;
import ch.bergturbenthal.wisp.manager.model.IpNetwork;
import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.NetworkInterface;
import ch.bergturbenthal.wisp.manager.model.RangePair;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.VLan;
import ch.bergturbenthal.wisp.manager.model.address.AddressRangeType;
import ch.bergturbenthal.wisp.manager.model.address.IpAddressType;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkInterfaceType;

@Slf4j
@Stateless
public class AddressManagementBean {
	@PersistenceContext
	private EntityManager entityManager;

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
		final IpRange reservationRange = new IpRange(new IpNetwork(new IpAddress(rangeAddress), rangeMask), reservationMask, AddressRangeType.ROOT);
		reservationRange.setComment(comment);
		entityManager.persist(reservationRange);
		return reservationRange;
	}

	private VLan appendVlan(final int vlanId, final RangePair parentAddresses) {
		final RangePair address = new RangePair();
		final VLan vLan = new VLan();
		vLan.setVlanId(Integer.valueOf(vlanId));
		address.setV4Address(reserveRange(parentAddresses.getV4Address(), AddressRangeType.ASSIGNED, 32, null));
		address.setV6Address(reserveRange(parentAddresses.getV6Address(), AddressRangeType.ASSIGNED, 128, null));
		vLan.setAddress(address);
		return vLan;
	}

	private CriteriaQuery<IpRange> createFindMatchingRangeQuery() {
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<IpRange> query = criteriaBuilder.createQuery(IpRange.class);
		final Root<IpRange> from = query.from(IpRange.class);
		final Path<AddressRangeType> typePath = from.get("type");
		final Path<IpNetwork> rangePath = from.get("range");
		final Path<IpAddressType> addressTypePath = rangePath.get("address").get("addressType");
		final Path<Integer> netmask = rangePath.get("netmask");
		final Path<Integer> rangeMaskPath = from.get("rangeMask");
		query.where(criteriaBuilder.equal(typePath, criteriaBuilder.parameter(AddressRangeType.class, "rangeType")),
								criteriaBuilder.equal(addressTypePath, criteriaBuilder.parameter(IpAddressType.class, "addressType")),
								criteriaBuilder.le(rangeMaskPath, criteriaBuilder.parameter(Integer.class, "maxNetSize")),
								criteriaBuilder.lt(netmask, rangeMaskPath));
		query.orderBy(criteriaBuilder.desc(rangeMaskPath));
		return query;
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

	public Connection fillConnection(final Connection originalConnection) {
		final Connection connection = entityManager.merge(originalConnection);
		final RangePair addresses;
		if (connection.getAddresses() == null) {
			addresses = new RangePair();
		} else {
			addresses = connection.getAddresses();
		}
		fillRangePair(addresses, AddressRangeType.CONNECTION, 29, 64, "Connection " + originalConnection.getId());
		connection.setAddresses(addresses);
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
			final RangePair address = new RangePair();
			fillRangePair(address, AddressRangeType.USER, 24, 64, null);
			vLan.setAddress(address);
			vLan.setStation(station);
			ownNetworks.add(vLan);
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
		fillRangePair(loopback, AddressRangeType.LOOPBACK, 32, 128, "Station " + station.getName());
		station.setLoopback(loopback);
	}

	private void fillNetworkDevice(final Station station) {
		final NetworkDevice networkDevice = station.getDevice();
		if (networkDevice == null) {
			return;
		}
		// collect unassigned interfaces and connections at this station
		final Set<Connection> remainingConnections = new HashSet<>(emptyIfNull(station.getBeginningConnections()));
		remainingConnections.addAll(emptyIfNull(station.getEndingConnections()));
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
			} else {
				// find connections and stations for this interface
				final Set<Connection> foundConnections = new HashSet<>();
				final Set<Station> foundStations = new HashSet<>();
				for (final VLan vLan : networks) {
					final RangePair connectionAddress = vLan.getAddress();
					foundStations.add(findStationForRange(connectionAddress.getV4Address().getParentRange()));
					foundStations.add(findStationForRange(connectionAddress.getV6Address().getParentRange()));
					foundConnections.add(findConnectionForRange(connectionAddress.getV4Address().getParentRange()));
					foundConnections.add(findConnectionForRange(connectionAddress.getV6Address().getParentRange()));
				}
				// remove not found entries
				foundConnections.remove(null);
				foundStations.remove(null);
				if (foundStations.isEmpty() && foundConnections.isEmpty()) {
					// unassigned interface
					freeInterfaces.add(networkInterface);
					continue;
				}
				if (foundStations.isEmpty() && foundConnections.size() == 1) {
					// correct connected interface
					final Connection connection = foundConnections.iterator().next();
					updateInterfaceTitle(networkInterface, connection);
					remainingConnections.remove(connection);
					continue;
				}
				if (foundStations.size() == 1 && foundConnections.isEmpty()) {
					final Station foundStation = foundStations.iterator().next();
					if (foundStation.equals(station)) {
						// locally connected interface
						networkInterface.setInterfaceName("home");
						userAssignedInterfaces.add(networkInterface);
						continue;
					}
				}
				// every other case is inconsistent and will be cleaned
				log.warn("Inconsisten connection at interface " + networkInterface + " cleaning");
				networkInterface.getNetworks().clear();
				freeInterfaces.add(networkInterface);
			}
		}
		// assign remaining interfaces to connections
		final Iterator<NetworkInterface> freeInterfacesIterator = freeInterfaces.iterator();
		final Iterator<Connection> unassignedConnectionsIterator = remainingConnections.iterator();
		while (freeInterfacesIterator.hasNext() && unassignedConnectionsIterator.hasNext()) {
			final NetworkInterface networkInterface = freeInterfacesIterator.next();
			if (networkInterface.getType() != NetworkInterfaceType.LAN) {
				// only lan interfaces
				continue;
			}
			if (userAssignedInterfaces.isEmpty()) {
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
				userAssignedInterfaces.add(networkInterface);
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
			updateInterfaceTitle(networkInterface, connection);
		}
	}

	private void fillRangePair(final RangePair pair, final AddressRangeType rangeType, final int v4Netmask, final int v6Netmask, final String comment) {
		if (pair.getV4Address() == null) {
			pair.setV4Address(findAndReserveAddressRange(rangeType, IpAddressType.V4, v4Netmask, AddressRangeType.ASSIGNED, comment));
		}
		if (pair.getV6Address() == null) {
			pair.setV6Address(findAndReserveAddressRange(rangeType, IpAddressType.V6, v6Netmask, AddressRangeType.ASSIGNED, comment));
		}
	}

	public Station fillStation(final Station originalStation) {
		final Station station = entityManager.merge(originalStation);
		fillLoopbackAddress(station);
		fillLanIfNone(station);
		fillNetworkDevice(station);
		return station;
	}

	public List<IpRange> findAllRootRanges() {
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<IpRange> query = criteriaBuilder.createQuery(IpRange.class);
		final Root<IpRange> from = query.from(IpRange.class);
		final Path<AddressRangeType> typePath = from.get("type");
		query.where(criteriaBuilder.equal(typePath, AddressRangeType.ROOT));

		final List<IpRange> resultList = entityManager.createQuery(query).getResultList();
		return resultList;
	}

	public IpRange findAndReserveAddressRange(final AddressRangeType rangeType,
																						final IpAddressType addressType,
																						final int maxNetSize,
																						final AddressRangeType typeOfReservation,
																						final String comment) {
		final IpRange parentRange = findMatchingRange(rangeType, addressType, maxNetSize);
		if (parentRange == null) {
			return null;
		}
		return reserveRange(parentRange, typeOfReservation == null ? AddressRangeType.ASSIGNED : typeOfReservation, maxNetSize, comment);
	}

	private Connection findConnectionForRange(final IpRange connectionRange) {
		final IpAddressType rangeType = connectionRange.getRange().getAddress().getAddressType();

		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Connection> query = criteriaBuilder.createQuery(Connection.class);
		final Root<Connection> connectionPath = query.from(Connection.class);
		final Path<IpRange> v4AddressPath = connectionPath.get("addresses").get(rangeType == IpAddressType.V4 ? "v4Address" : "v6Address");
		query.where(criteriaBuilder.equal(v4AddressPath, criteriaBuilder.parameter(IpRange.class, "connectionRange")));

		final TypedQuery<Connection> typedQuery = entityManager.createQuery(query);
		typedQuery.setParameter("connectionRange", connectionRange);
		typedQuery.setMaxResults(1);
		final List<Connection> results = typedQuery.getResultList();
		if (results.isEmpty()) {
			return null;
		} else {
			return results.get(0);
		}
	}

	private IpRange findMatchingRange(final AddressRangeType rangeType, final IpAddressType addressType, final int maxNetSize) {
		final CriteriaQuery<IpRange> query = createFindMatchingRangeQuery();

		final TypedQuery<IpRange> typedQuery = entityManager.createQuery(query);
		typedQuery.setParameter("rangeType", rangeType);
		typedQuery.setParameter("addressType", addressType);
		typedQuery.setParameter("maxNetSize", Integer.valueOf(maxNetSize));
		for (final IpRange range : typedQuery.getResultList()) {
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
		final IpAddressType rangeType = range.getRange().getAddress().getAddressType();

		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Station> query = criteriaBuilder.createQuery(Station.class);
		final Root<Station> connectionPath = query.from(Station.class);
		final Path<IpRange> addressPath = connectionPath.get("ownNetworks").get("address").get(rangeType == IpAddressType.V4 ? "v4Address" : "v6Address");
		query.where(criteriaBuilder.equal(addressPath, criteriaBuilder.parameter(IpRange.class, "range")));

		final TypedQuery<Station> typedQuery = entityManager.createQuery(query);
		typedQuery.setParameter("connectionRange", range);
		typedQuery.setMaxResults(1);
		final List<Station> results = typedQuery.getResultList();
		if (results.isEmpty()) {
			return null;
		} else {
			return results.get(0);
		}
	}

	public void initAddressRanges() {
		try {
			final List<IpRange> resultList = findAllRootRanges();
			// System.out.println("Ranges: " + resultList);
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
				System.out.println("v4-Network: " + ipV4ReservationRange);
				System.out.println("v6-Network: " + ipV6ReservationRange);
			}
			for (final IpRange ipV4ReservationRange : resultList) {
				System.out.println("Range: " + ipV4ReservationRange);
			}
		} catch (final UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	public IpRange reserveRange(final IpRange parentRange, final AddressRangeType type, final int mask, final String comment) {
		if (mask < parentRange.getRangeMask()) {
			throw new IllegalArgumentException("To big range: " + mask + " parent allowes " + parentRange.getRangeMask());
		}
		final boolean isV4 = parentRange.getRange().getAddress().getAddressType() == IpAddressType.V4;
		final BigInteger parentRangeStartAddress = parentRange.getRange().getAddress().getRawValue();
		final BigInteger rangeSize = BigInteger.valueOf(1).shiftLeft((isV4 ? 32 : 128) - parentRange.getRangeMask());
		// for single v4-address -> skip first and last address
		final boolean isV4SingleAddress = mask == 32 && isV4;
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
			entityManager.persist(newRange);
			System.out.println("Reserved: " + newRange);
			return newRange;
		}
		// no free reservation found in range
		return null;
	}

	private void updateInterfaceTitle(final NetworkInterface networkInterface, final Connection connection) {
		networkInterface.setInterfaceName("connection: " + connection.getTitle());
	}
}
