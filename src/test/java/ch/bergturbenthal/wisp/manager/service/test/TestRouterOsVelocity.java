package ch.bergturbenthal.wisp.manager.service.test;

import java.net.UnknownHostException;

import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.Position;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.service.AddressManagementBean;
import ch.bergturbenthal.wisp.manager.service.ConnectionService;
import ch.bergturbenthal.wisp.manager.service.ConnectionServiceBean;
import ch.bergturbenthal.wisp.manager.service.NetworkDeviceManagementBean;
import ch.bergturbenthal.wisp.manager.service.StationService;
import ch.bergturbenthal.wisp.manager.service.StationServiceBean;
import ch.bergturbenthal.wisp.manager.service.TestHelperBean;
import ch.bergturbenthal.wisp.manager.service.provision.FirmwareCache;
import ch.bergturbenthal.wisp.manager.service.provision.routeros.ProvisionRouterOs;

@RunWith(Arquillian.class)
public class TestRouterOsVelocity {
	@Deployment
	public static JavaArchive createDeployment() {
		return ShrinkWrap.create(JavaArchive.class)
											.addClass(AddressManagementBean.class)
											.addClass(StationServiceBean.class)
											.addClass(TestHelperBean.class)
											.addClass(ProvisionRouterOs.class)
											.addClass(FirmwareCache.class)
											.addClass(NetworkDeviceManagementBean.class)
											.addClass(ConnectionServiceBean.class)
											.addAsResource("META-INF/persistence.xml")
											.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
	}

	@EJB
	private AddressManagementBean addressManagementBean;
	@EJB
	private ConnectionService connectionService;
	@EJB
	private NetworkDeviceManagementBean networkDeviceManagementBean;
	@EJB
	private StationService stationService;

	@EJB
	private TestHelperBean testHelperBean;

	private NetworkDevice createStationWithDevice(final String serial, final String macAddress, final String name) {
		final Station station = stationService.addStation(new Position(47.4212786, 8.8859975));
		final NetworkDevice device = NetworkDevice.createDevice(NetworkDeviceModel.RB750GL, macAddress);
		station.setDevice(device);
		station.setName(name);
		device.setSerialNumber(serial);
		device.setStation(station);
		stationService.updateStation(station);
		return device;
	}

	@Before
	public void initData() throws UnknownHostException {
		testHelperBean.clearData();
		testHelperBean.initAddressRanges();
	}

	@Test
	public void testGenerateRbConfig() throws UnknownHostException {
		final NetworkDevice d1 = createStationWithDevice("3B050205B659", "d4ca6dd444f3", "Berg");
		final NetworkDevice d2 = createStationWithDevice("3B05027CF736", "d4ca6db5e9e7", "Chalchegg");
		final Connection connection = connectionService.connectStations(d1.getStation(), d2.getStation());
		System.out.println(networkDeviceManagementBean.generateConfig(stationService.findStation(d1.getStation().getId()).getDevice()));
		System.out.println(networkDeviceManagementBean.generateConfig(stationService.findStation(d2.getStation().getId()).getDevice()));
		// networkDeviceManagementBean.loadConfig(device, InetAddress.getByName("192.168.88.1"));
	}

}
