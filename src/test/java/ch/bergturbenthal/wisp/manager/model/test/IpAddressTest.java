package ch.bergturbenthal.wisp.manager.model.test;

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

import ch.bergturbenthal.wisp.manager.model.address.AddressRangeType;
import ch.bergturbenthal.wisp.manager.model.address.IpAddressType;
import ch.bergturbenthal.wisp.manager.service.AddressManagementBean;
import ch.bergturbenthal.wisp.manager.service.TestHelperBean;

@RunWith(Arquillian.class)
public class IpAddressTest {
	@Deployment
	public static JavaArchive createDeployment() {
		return ShrinkWrap.create(JavaArchive.class)
											.addClass(AddressManagementBean.class)
											.addClass(TestHelperBean.class)
											.addAsResource("META-INF/persistence.xml")
											.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
	}

	@EJB
	private AddressManagementBean addressManagementBean;
	@EJB
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
		return addressManagementBean.findAndReserveAddressRange(rangeType, addressType, maskLength, AddressRangeType.ASSIGNED, null).getRange().getAddress().getInetAddress();
	}

	@Test
	public void testIpv4ConnectionReservationAddress() throws UnknownHostException {
		Assert.assertEquals(InetAddress.getByName("172.16.0.0"), nextRange(AddressRangeType.CONNECTION, IpAddressType.V4, 30));
		Assert.assertEquals(InetAddress.getByName("172.16.0.8"), nextRange(AddressRangeType.CONNECTION, IpAddressType.V4, 29));
		Assert.assertEquals(InetAddress.getByName("172.16.0.16"), nextRange(AddressRangeType.CONNECTION, IpAddressType.V4, 30));
	}

	@Test
	public void testIpv4ConnectionReservationAddress2() throws UnknownHostException {
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
