package ch.bergturbenthal.wisp.manager.model.test;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.model.address.AddressRangeType;
import ch.bergturbenthal.wisp.manager.model.address.IpAddressType;
import ch.bergturbenthal.wisp.manager.service.AddressManagementBean;

@RunWith(Arquillian.class)
public class IpAddressTest {
	@Deployment
	public static JavaArchive createDeployment() {
		return ShrinkWrap.create(JavaArchive.class)
											.addClass(AddressManagementBean.class)
											.addAsResource("META-INF/persistence.xml")
											.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
	}

	@EJB
	private AddressManagementBean addressManagementBean;

	@Before
	public void initAddresses() throws UnknownHostException {
		if (addressManagementBean.findAllRootRanges().size() > 0) {
			return;
		}
		final IpRange rootV4 = addressManagementBean.addRootRange(Inet4Address.getByName("172.16.0.0"), 12, 16, "Internal v4 Range");
		final IpRange smallV4Ranges = addressManagementBean.reserveRange(rootV4, AddressRangeType.ADMINISTRATIVE, 24, null);
		addressManagementBean.reserveRange(smallV4Ranges, AddressRangeType.LOOPBACK, 32, null);
		addressManagementBean.reserveRange(smallV4Ranges, AddressRangeType.CONNECTION, 29, null);

		final IpRange ipV6ReservationRange = addressManagementBean.addRootRange(Inet6Address.getByName("fd7e:907d:34ab::"), 48, 56, "Internal v6 Range");
		final IpRange singleRanges = addressManagementBean.reserveRange(ipV6ReservationRange, AddressRangeType.ADMINISTRATIVE, 64, "Ranges for single addresses");
		addressManagementBean.reserveRange(singleRanges, AddressRangeType.LOOPBACK, 128, null);
		addressManagementBean.reserveRange(ipV6ReservationRange, AddressRangeType.CONNECTION, 64, null);

	}

	private InetAddress nextAddress(final AddressRangeType rangeType, final IpAddressType addressType) {
		return nextRange(rangeType, addressType, addressType == IpAddressType.V4 ? 32 : 128);
	}

	private InetAddress nextRange(final AddressRangeType rangeType, final IpAddressType addressType, final int maskLength) {
		return addressManagementBean.findAndReserveAddressRange(rangeType, addressType, maskLength, AddressRangeType.ASSIGNED, null).getRange().getAddress().getInetAddress();
	}

	@Test
	public void testIpv4ConnectionReservationAddress() throws UnknownHostException {
		Assert.assertEquals(InetAddress.getByName("172.16.0.0"), nextRange(AddressRangeType.CONNECTION, IpAddressType.V4, 30));
		Assert.assertEquals(InetAddress.getByName("172.16.0.8"), nextRange(AddressRangeType.CONNECTION, IpAddressType.V4, 29));
		Assert.assertEquals(InetAddress.getByName("172.16.0.16"), nextRange(AddressRangeType.CONNECTION, IpAddressType.V4, 30));
	}

	@Test
	public void testIpv4LoopbackReservationAddress() throws UnknownHostException {
		Assert.assertEquals(InetAddress.getByName("172.16.1.1"), nextAddress(AddressRangeType.LOOPBACK, IpAddressType.V4));
		Assert.assertEquals(InetAddress.getByName("172.16.1.2"), nextAddress(AddressRangeType.LOOPBACK, IpAddressType.V4));
		Assert.assertEquals(InetAddress.getByName("172.16.1.3"), nextAddress(AddressRangeType.LOOPBACK, IpAddressType.V4));
	}

	@Test
	public void testIpv6ConnectionReservationAddress() throws UnknownHostException {
		Assert.assertEquals(InetAddress.getByName("fd7e:907d:34ab:100::"), nextAddress(AddressRangeType.CONNECTION, IpAddressType.V6));
		Assert.assertEquals(InetAddress.getByName("fd7e:907d:34ab:101::"), nextAddress(AddressRangeType.CONNECTION, IpAddressType.V6));
		Assert.assertEquals(InetAddress.getByName("fd7e:907d:34ab:102::"), nextAddress(AddressRangeType.CONNECTION, IpAddressType.V6));
	}

	@Test
	public void testIpv6LoopbackReservationAddress() throws UnknownHostException {
		Assert.assertEquals(InetAddress.getByName("fd7e:907d:34ab::"), nextAddress(AddressRangeType.LOOPBACK, IpAddressType.V6));
		Assert.assertEquals(InetAddress.getByName("fd7e:907d:34ab::1"), nextAddress(AddressRangeType.LOOPBACK, IpAddressType.V6));
		Assert.assertEquals(InetAddress.getByName("fd7e:907d:34ab::2"), nextAddress(AddressRangeType.LOOPBACK, IpAddressType.V6));
	}

}
