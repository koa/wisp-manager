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
import ch.bergturbenthal.wisp.manager.model.IpReservationRange;
import ch.bergturbenthal.wisp.manager.model.address.AddressRangeType;

@Stateless
public class AddressManagementBean {
	@PersistenceContext
	private EntityManager entityManager;

	private List<IpReservationRange> findAllV4RootRanges() {
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<IpReservationRange> query = criteriaBuilder.createQuery(IpReservationRange.class);
		final Root<IpReservationRange> from = query.from(IpReservationRange.class);
		final Path<AddressRangeType> typePath = from.get("type");
		query.where(criteriaBuilder.equal(typePath, AddressRangeType.ROOT));

		final List<IpReservationRange> resultList = entityManager.createQuery(query).getResultList();
		return resultList;
	}

	public void initAddressRanges() {
		try {
			final List<IpReservationRange> resultList = findAllV4RootRanges();
			// System.out.println("Ranges: " + resultList);
			if (resultList.isEmpty()) {
				final IpReservationRange ipV4ReservationRange = new IpReservationRange(	new IpNetwork(new IpAddress(Inet4Address.getByName("172.16.0.0")), 12),
																																								16,
																																								AddressRangeType.ROOT);
				entityManager.persist(ipV4ReservationRange);
				final IpReservationRange smallRanges = reserveRangeInternal(ipV4ReservationRange, AddressRangeType.ADMINISTRATIVE, 24);
				smallRanges.setComment("Some small Ranges");
				reserveRangeInternal(smallRanges, AddressRangeType.LOOPBACK, 32);
				// System.out.println("Added network: " + ipV4Network);
				final IpReservationRange ipV6ReservationRange = new IpReservationRange(	new IpNetwork(new IpAddress(Inet6Address.getByName("fd7e:907d:34ab::")), 48),
																																								56,
																																								AddressRangeType.ROOT);
				ipV4ReservationRange.setComment("Site Local Range");
				entityManager.persist(ipV6ReservationRange);
				System.out.println("v6-Network: " + ipV6ReservationRange);
			}
			for (final IpReservationRange ipV4ReservationRange : resultList) {
				System.out.println("Range: " + ipV4ReservationRange);
			}
		} catch (final UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	public IpReservationRange reserveRange(final IpReservationRange parentRange, final AddressRangeType type, final int mask) {
		return reserveRangeInternal(entityManager.merge(parentRange), type, mask);
	}

	private IpReservationRange reserveRangeInternal(final IpReservationRange parentRange, final AddressRangeType type, final int mask) {
		if (mask < parentRange.getRangeMask()) {
			throw new IllegalArgumentException("To big range: " + mask + " parent allowes " + parentRange.getRangeMask());
		}
		final BigInteger parentRangeStartAddress = parentRange.getRange().getAddress().getRawValue();
		final BigInteger rangeSize = BigInteger.valueOf(1).shiftLeft(parentRange.getRangeMask());
		final int availableReservations = parentRange.getAvailableReservations();
		nextReservation:
		for (int i = 0; i < availableReservations; i++) {
			final BigInteger candidateAddress = parentRangeStartAddress.add(rangeSize.multiply(BigInteger.valueOf(i)));
			final Collection<IpReservationRange> reservations = parentRange.getReservations();
			for (final IpReservationRange reservationRange : reservations) {
				if (reservationRange.getRange().getAddress().getRawValue().equals(candidateAddress)) {
					continue nextReservation;
				}
			}
			// reservation is free
			final IpReservationRange newRange = new IpReservationRange(new IpNetwork(new IpAddress(candidateAddress), parentRange.getRangeMask()), mask, type);
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
