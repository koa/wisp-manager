package ch.bergturbenthal.wisp.manager.service;

import java.net.InetAddress;
import java.util.Collection;
import java.util.List;

import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.IpAddress;
import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.address.AddressRangeType;
import ch.bergturbenthal.wisp.manager.model.address.IpAddressType;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;

public interface AddressManagementService {

	void addGlobalDns(final IpAddress address);

	IpRange addRootRange(final InetAddress rangeAddress, final int rangeMask, final int reservationMask, final String comment);

	Connection fillConnection(final Connection originalConnection);

	Station fillStation(final Station originalStation);

	List<IpRange> findAllRootRanges();

	IpRange findAndReserveAddressRange(	final AddressRangeType rangeType,
																			final IpAddressType addressType,
																			final int maxNetSize,
																			final int nextDistributionSize,
																			final AddressRangeType typeOfReservation,
																			final String comment);

	void initAddressRanges();

	Collection<IpAddress> listGlobalDnsServers();

	void removeGlobalDns(final IpAddress address);

	IpRange reserveRange(final IpRange parentRange, final AddressRangeType type, final int mask, final String comment);

	CrudRepositoryContainer<IpRange, Long> createIpContainer();

	String describeRangeUser(final IpRange range);

}