package ch.bergturbenthal.wisp.manager.service.test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ch.bergturbenthal.wisp.manager.WispManager;
import ch.bergturbenthal.wisp.manager.model.IpAddress;
import ch.bergturbenthal.wisp.manager.model.IpNetwork;
import ch.bergturbenthal.wisp.manager.repository.IpRangeRepository;
import ch.bergturbenthal.wisp.manager.service.AddressManagementService;
import ch.bergturbenthal.wisp.manager.service.TestHelperBean;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = WispManager.class)
public class AddressManagementTest {

	@Autowired
	private AddressManagementService addressManagementBean;
	@Autowired
	private IpRangeRepository ipRangeRepository;
	@Autowired
	private TestHelperBean testHelperBean;

	@Before
	public void initData() throws UnknownHostException {
		testHelperBean.clearData();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testV4AddressOverlap() throws UnknownHostException {
		addressManagementBean.addRootRange(new IpNetwork(new IpAddress(InetAddress.getByName("10.0.0.0")), 8), 16, "Big reservation");
		addressManagementBean.addRootRange(new IpNetwork(new IpAddress(InetAddress.getByName("10.14.20.0")), 24), 28, "Small reservation");
	}

	@Test
	public void testV4NonAddressOverlap() throws UnknownHostException {
		addressManagementBean.addRootRange(new IpNetwork(new IpAddress(InetAddress.getByName("10.13.0.0")), 16), 20, "Big reservation");
		addressManagementBean.addRootRange(new IpNetwork(new IpAddress(InetAddress.getByName("10.14.20.0")), 24), 28, "Small reservation");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testV6AddressOverlap() throws UnknownHostException {
		addressManagementBean.addRootRange(new IpNetwork(new IpAddress(InetAddress.getByName("fd7e:907d:34ab::")), 48), 64, "Big reservation");
		addressManagementBean.addRootRange(new IpNetwork(new IpAddress(InetAddress.getByName("fd7e:907d:34ab:200::")), 56), 64, "Small reservation");
	}

	@Test
	public void testV6NonAddressOverlap() throws UnknownHostException {
		addressManagementBean.addRootRange(new IpNetwork(new IpAddress(InetAddress.getByName("fd7e:907d:34ab::")), 56), 64, "Big reservation");
		addressManagementBean.addRootRange(new IpNetwork(new IpAddress(InetAddress.getByName("fd7e:907d:34ab:200::")), 56), 64, "Small reservation");
	}
}
