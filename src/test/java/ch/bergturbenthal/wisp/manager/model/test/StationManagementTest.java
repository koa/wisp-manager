package ch.bergturbenthal.wisp.manager.model.test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.transaction.Transactional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ch.bergturbenthal.wisp.manager.WispManager;
import ch.bergturbenthal.wisp.manager.model.CustomerConnection;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.NetworkInterface;
import ch.bergturbenthal.wisp.manager.model.RangePair;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.VLan;
import ch.bergturbenthal.wisp.manager.service.AddressManagementService;
import ch.bergturbenthal.wisp.manager.service.StationService;
import ch.bergturbenthal.wisp.manager.service.TestHelperBean;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = WispManager.class)
public class StationManagementTest {

	@Autowired
	private AddressManagementService addressManagementBean;
	@Autowired
	private StationService stationService;

	@Autowired
	private TestHelperBean testHelperBean;

	@Before
	@Transactional
	public void initData() throws UnknownHostException {
		testHelperBean.clearData();
		testHelperBean.initAddressRanges();
	}

	@Test
	@Transactional
	public void testCreateStation() throws UnknownHostException {
		final NetworkDevice device = testHelperBean.createStationWithDevice("serial", "80:ee:73:67:df:16", "name", false);
		final Station station = stationService.fillStation(device.getStation());

		Assert.assertEquals(InetAddress.getByName("172.16.0.1"), station.getLoopback().getV4Address().getRange().getAddress().getInetAddress());
		Assert.assertEquals(InetAddress.getByName("fd7e:907d:34ab::"), station.getLoopback().getV6Address().getRange().getAddress().getInetAddress());
		final CustomerConnection customerConnection = station.getCustomerConnections().iterator().next();
		final VLan vlan = customerConnection.getOwnNetworks().iterator().next();
		Assert.assertEquals(0, vlan.getVlanId());
		final RangePair networkAddress = vlan.getAddress();
		Assert.assertEquals(InetAddress.getByName("172.17.0.0"), networkAddress.getInet4Address());
		Assert.assertEquals(InetAddress.getByName("fd7e:907d:34ab:200::"), networkAddress.getInet6Address());

		final NetworkInterface homeInterface = station.getDevice().getInterfaces().get(0);
		final VLan deviceAddressVlan = homeInterface.getCustomerConnection().getOwnNetworks().iterator().next();
		Assert.assertEquals(0, deviceAddressVlan.getVlanId());
		final RangePair interfaceAddress = deviceAddressVlan.getAddress();
		Assert.assertEquals(InetAddress.getByName("172.17.0.0"), interfaceAddress.getInet4Address());
		Assert.assertEquals(InetAddress.getByName("fd7e:907d:34ab:200::"), interfaceAddress.getInet6Address());
	}

}
