package ch.bergturbenthal.wisp.manager.model.test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ch.bergturbenthal.wisp.manager.WispManager;
import ch.bergturbenthal.wisp.manager.model.address.AddressRangeType;
import ch.bergturbenthal.wisp.manager.model.address.IpAddressType;
import ch.bergturbenthal.wisp.manager.service.AddressManagementService;
import ch.bergturbenthal.wisp.manager.service.TestHelperBean;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = WispManager.class)
public class IpAddressManagementTest {

	@Autowired
	private AddressManagementService addressManagementBean;
	@Autowired
	private TestHelperBean testHelperBean;

	@Before
	public void initData() throws UnknownHostException {
		testHelperBean.clearData();
		testHelperBean.initAddressRanges();
	}

	private InetAddress nextAddress(final AddressRangeType rangeType, final IpAddressType addressType) {
		return nextRange(rangeType, addressType, addressType == IpAddressType.V4 ? 32 : 128);
	}

	private InetAddress nextRange(final AddressRangeType rangeType, final IpAddressType addressType, final int maskLength) {
		return addressManagementBean.findAndReserveAddressRange(rangeType, addressType, maskLength, maskLength, AddressRangeType.ASSIGNED, null)
																.getRange()
																.getAddress()
																.getInetAddress();
	}

	@Test
	public void testIpv4ConnectionReservationAddress() throws UnknownHostException {
		Assert.assertEquals(InetAddress.getByName("172.16.1.0"), nextRange(AddressRangeType.CONNECTION, IpAddressType.V4, 30));
		Assert.assertEquals(InetAddress.getByName("172.16.1.8"), nextRange(AddressRangeType.CONNECTION, IpAddressType.V4, 29));
		Assert.assertEquals(InetAddress.getByName("172.16.1.16"), nextRange(AddressRangeType.CONNECTION, IpAddressType.V4, 30));
	}

	@Test
	public void testIpv4ConnectionReservationAddress2() throws UnknownHostException {
		Assert.assertEquals(InetAddress.getByName("172.16.1.0"), nextRange(AddressRangeType.CONNECTION, IpAddressType.V4, 30));
		Assert.assertEquals(InetAddress.getByName("172.16.1.8"), nextRange(AddressRangeType.CONNECTION, IpAddressType.V4, 29));
		Assert.assertEquals(InetAddress.getByName("172.16.1.16"), nextRange(AddressRangeType.CONNECTION, IpAddressType.V4, 30));
	}

	@Test
	public void testIpv4LoopbackReservationAddress() throws UnknownHostException {
		Assert.assertEquals(InetAddress.getByName("172.16.0.1"), nextAddress(AddressRangeType.LOOPBACK, IpAddressType.V4));
		Assert.assertEquals(InetAddress.getByName("172.16.0.2"), nextAddress(AddressRangeType.LOOPBACK, IpAddressType.V4));
		Assert.assertEquals(InetAddress.getByName("172.16.0.3"), nextAddress(AddressRangeType.LOOPBACK, IpAddressType.V4));
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
