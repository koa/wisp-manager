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

public interface AddressManagementService {

	public abstract void addGlobalDns(final IpAddress address);

	public abstract IpRange addRootRange(final InetAddress rangeAddress, final int rangeMask, final int reservationMask, final String comment);

	public abstract Connection fillConnection(final Connection originalConnection);

	public abstract Station fillStation(final Station originalStation);

	public abstract List<IpRange> findAllRootRanges();

	public abstract IpRange findAndReserveAddressRange(	final AddressRangeType rangeType,
																											final IpAddressType addressType,
																											final int maxNetSize,
																											final int nextDistributionSize,
																											final AddressRangeType typeOfReservation,
																											final String comment);

	public abstract void initAddressRanges();

	public abstract Collection<IpAddress> listGlobalDnsServers();

	public abstract void removeGlobalDns(final IpAddress address);

	public abstract IpRange reserveRange(final IpRange parentRange, final AddressRangeType type, final int mask, final String comment);

}