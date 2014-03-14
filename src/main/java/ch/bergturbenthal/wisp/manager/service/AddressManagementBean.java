package ch.bergturbenthal.wisp.manager.service;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.IpAddress;
import ch.bergturbenthal.wisp.manager.model.IpNetwork;
import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.model.RangePair;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.VLan;
import ch.bergturbenthal.wisp.manager.model.address.AddressRangeType;
import ch.bergturbenthal.wisp.manager.model.address.IpAddressType;

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

	private void fillRangePair(final RangePair pair, final AddressRangeType rangeType, final int v4Netmask, final int v6Netmask, final String comment) {
		if (pair.getV4Address() == null) {
			pair.setV4Address(findAndReserveAddressRange(rangeType, IpAddressType.V4, v4Netmask, AddressRangeType.ASSIGNED, comment));
		}
		if (pair.getV6Address() == null) {
			pair.setV6Address(findAndReserveAddressRange(rangeType, IpAddressType.V6, v6Netmask, AddressRangeType.ASSIGNED, comment));
		}
	}

	public Station fillStation(final Station station) {
		final Station mergedStation = entityManager.merge(station);
		final RangePair loopback;
		if (mergedStation.getLoopback() == null) {
			loopback = new RangePair();
		} else {
			loopback = mergedStation.getLoopback();
		}
		fillRangePair(loopback, AddressRangeType.LOOPBACK, 32, 128, "Station " + station.getName());
		mergedStation.setLoopback(loopback);

		Set<VLan> ownNetworks;
		if (mergedStation.getOwnNetworks() == null) {
			ownNetworks = new HashSet<>();
		} else {
			ownNetworks = mergedStation.getOwnNetworks();
		}
		if (ownNetworks.isEmpty()) {
			final VLan vLan = new VLan();
			vLan.setVlanId(0);
			final RangePair address = new RangePair();
			fillRangePair(address, AddressRangeType.USER, 24, 64, null);
			vLan.setAddress(address);
			vLan.setStation(mergedStation);
			ownNetworks.add(vLan);
		}
		mergedStation.setOwnNetworks(ownNetworks);
		return mergedStation;
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

	private IpRange findMatchingRange(final AddressRangeType rangeType, final IpAddressType addressType, final int maxNetSize) {
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<IpRange> query = criteriaBuilder.createQuery(IpRange.class);
		final Root<IpRange> from = query.from(IpRange.class);
		final Path<AddressRangeType> typePath = from.get("type");
		final Path<IpNetwork> rangePath = from.get("range");
		final Path<IpAddressType> addressTypePath = rangePath.get("address").get("addressType");
		final Path<Integer> netmask = rangePath.get("netmask");
		final Path<Integer> rangeMaskPath = from.get("rangeMask");
		query.where(criteriaBuilder.equal(typePath, rangeType),
								criteriaBuilder.equal(addressTypePath, addressType),
								criteriaBuilder.le(rangeMaskPath, Integer.valueOf(maxNetSize)),
								criteriaBuilder.lt(netmask, rangeMaskPath));
		query.orderBy(criteriaBuilder.desc(rangeMaskPath));

		for (final IpRange range : entityManager.createQuery(query).getResultList()) {
			if (range.getAvailableReservations() <= range.getReservations().size()) {
				// range full
				continue;
			}
			return range;
		}
		// no matching range found
		return null;
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
}
