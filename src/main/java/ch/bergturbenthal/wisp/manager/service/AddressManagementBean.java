package ch.bergturbenthal.wisp.manager.service;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import ch.bergturbenthal.wisp.manager.model.IpAddress;
import ch.bergturbenthal.wisp.manager.model.IpNetwork;
import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.model.RangePair;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.address.AddressRangeType;
import ch.bergturbenthal.wisp.manager.model.address.IpAddressType;

@Stateless
public class AddressManagementBean {
	@PersistenceContext
	private EntityManager entityManager;

	public void fillStation(final Station station) {
		final Station mergedStation = entityManager.merge(station);
		if (mergedStation.getLoopback() == null) {
			mergedStation.setLoopback(new RangePair());
		}
		final RangePair loopback = mergedStation.getLoopback();
		if (loopback.getV4Address() == null) {
			final IpRange v4Parent = findMatchingRange(AddressRangeType.LOOPBACK, IpAddressType.V4, 32);
			final IpRange v4Loopbackip = reserveRangeInternal(v4Parent, AddressRangeType.LOOPBACK, 32);
			loopback.setV4Address(v4Loopbackip);
		}
		if (loopback.getV6Address() == null) {
			final IpRange v6Parent = findMatchingRange(AddressRangeType.LOOPBACK, IpAddressType.V6, 128);
			final IpRange v6LoopbackIp = reserveRangeInternal(v6Parent, AddressRangeType.LOOPBACK, 128);
			loopback.setV6Address(v6LoopbackIp);
		}
	}

	private List<IpRange> findAllRootRanges() {
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<IpRange> query = criteriaBuilder.createQuery(IpRange.class);
		final Root<IpRange> from = query.from(IpRange.class);
		final Path<AddressRangeType> typePath = from.get("type");
		query.where(criteriaBuilder.equal(typePath, AddressRangeType.ROOT));

		final List<IpRange> resultList = entityManager.createQuery(query).getResultList();
		return resultList;
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
				final IpRange ipV4ReservationRange = new IpRange(new IpNetwork(new IpAddress(Inet4Address.getByName("172.16.0.0")), 12), 16, AddressRangeType.ROOT);
				entityManager.persist(ipV4ReservationRange);
				final IpRange smallV4Ranges = reserveRangeInternal(ipV4ReservationRange, AddressRangeType.ADMINISTRATIVE, 24);
				smallV4Ranges.setComment("Some small Ranges");
				reserveRangeInternal(smallV4Ranges, AddressRangeType.LOOPBACK, 32);
				reserveRangeInternal(smallV4Ranges, AddressRangeType.CONNECTION, 29);
				// System.out.println("Added network: " + ipV4Network);
				final IpRange ipV6ReservationRange = new IpRange(new IpNetwork(new IpAddress(Inet6Address.getByName("fd7e:907d:34ab::")), 48), 56, AddressRangeType.ROOT);
				ipV6ReservationRange.setComment("Site Local Range");
				final IpRange singleRanges = reserveRangeInternal(ipV6ReservationRange, AddressRangeType.ADMINISTRATIVE, 64);
				singleRanges.setComment("Ranges for single addresses");
				reserveRangeInternal(singleRanges, AddressRangeType.LOOPBACK, 128);
				reserveRangeInternal(ipV6ReservationRange, AddressRangeType.CONNECTION, 64);
				entityManager.persist(ipV6ReservationRange);
				System.out.println("v6-Network: " + ipV6ReservationRange);
			}
			for (final IpRange ipV4ReservationRange : resultList) {
				System.out.println("Range: " + ipV4ReservationRange);
			}
		} catch (final UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	public IpRange reserveRange(final IpRange parentRange, final AddressRangeType type, final int mask) {
		return reserveRangeInternal(entityManager.merge(parentRange), type, mask);
	}

	private IpRange reserveRangeInternal(final IpRange parentRange, final AddressRangeType type, final int mask) {
		if (mask < parentRange.getRangeMask()) {
			throw new IllegalArgumentException("To big range: " + mask + " parent allowes " + parentRange.getRangeMask());
		}
		final boolean isV4 = parentRange.getRange().getAddress().getAddressType() == IpAddressType.V4;
		final BigInteger parentRangeStartAddress = parentRange.getRange().getAddress().getRawValue();
		final BigInteger rangeSize = BigInteger.valueOf(1).shiftLeft((isV4 ? 32 : 128) - parentRange.getRangeMask());
		// for single v4-address -> skip first and last address
		final boolean isV4SingleAddress = mask == 32 && isV4;
		final int availableReservations = isV4SingleAddress ? parentRange.getAvailableReservations() - 1 : parentRange.getAvailableReservations();
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
			entityManager.persist(newRange);
			System.out.println("Reserved: " + newRange);
			return newRange;
		}
		// no free reservation found in range
		return null;
	}
}
