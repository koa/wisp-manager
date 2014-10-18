package ch.bergturbenthal.wisp.manager.service;

import java.net.InetAddress;
import java.util.Collection;
import java.util.List;

import ch.bergturbenthal.wisp.manager.model.IpAddress;
import ch.bergturbenthal.wisp.manager.model.IpNetwork;
import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.model.PortExpose;
import ch.bergturbenthal.wisp.manager.model.RangePair;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.VLan;
import ch.bergturbenthal.wisp.manager.model.address.AddressRangeType;
import ch.bergturbenthal.wisp.manager.model.address.IpAddressType;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;

public interface AddressManagementService {

	void addGlobalDns(final IpAddress address);

	IpRange addRootRange(final IpNetwork reserveNetwork, final int reservationMask, final String comment);

	Station fillStation(final Station originalStation);

	List<IpRange> findAllRootRanges();

	IpRange findAndReserveAddressRange(	final AddressRangeType rangeType,
																			final IpAddressType addressType,
																			final int maxNetSize,
																			final int nextDistributionSize,
																			final AddressRangeType typeOfReservation,
																			final String comment);

	void initAddressRanges();

	void removeRange(final IpRange range);

	void removeRangeUsage(final IpRange range);

	Collection<IpAddress> listGlobalDnsServers();

	void removeGlobalDns(final IpAddress address);

	IpRange reserveRange(final IpRange parentRange, final AddressRangeType type, final int mask, final String comment);

	CrudRepositoryContainer<IpRange, Long> createIpContainer();

	String describeRangeUser(final IpRange range);

	Iterable<InetAddress> listPossibleNetworkDevices();

	boolean setAddressManually(final RangePair addressPair, final String address, final IpAddressType addressType);

	String getDhcpStartAddress(final VLan vlan);

	String getDhcpEndAddress(final VLan vlan);

	boolean setDhcpStartAddress(final VLan vlan, final String startAddress);

	boolean setDhcpEndAddress(final VLan vlan, final String endAddress);

	void cleanupOrphanRanges();

	void addPortExposition(final VLan vlan, final int port, final String address);

	void removePortExpostion(final PortExpose exposition);

}