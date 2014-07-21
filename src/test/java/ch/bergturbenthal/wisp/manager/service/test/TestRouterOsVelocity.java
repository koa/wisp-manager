package ch.bergturbenthal.wisp.manager.service.test;

import java.net.UnknownHostException;

import javax.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ch.bergturbenthal.wisp.manager.WispManager;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.service.AddressManagementService;
import ch.bergturbenthal.wisp.manager.service.ConnectionService;
import ch.bergturbenthal.wisp.manager.service.DemoSetupService;
import ch.bergturbenthal.wisp.manager.service.NetworkDeviceManagementService;
import ch.bergturbenthal.wisp.manager.service.StationService;
import ch.bergturbenthal.wisp.manager.service.TestHelperBean;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = WispManager.class)
@Transactional
public class TestRouterOsVelocity {

	@Autowired
	private AddressManagementService addressManagementBean;
	@Autowired
	private ConnectionService connectionService;
	@Autowired
	private DemoSetupService demoSetupService;
	@Autowired
	private NetworkDeviceManagementService networkDeviceManagementBean;
	@Autowired
	private StationService stationService;
	@Autowired
	private TestHelperBean testHelperBean;

	@Before
	public void initData() throws UnknownHostException {
		testHelperBean.clearData();
		demoSetupService.initDemoData();
		for (final Station station : stationService.listAllStations()) {
			demoSetupService.fillDummyDevice(station);
			addressManagementBean.fillStation(station);
			log.info(networkDeviceManagementBean.generateConfig(station.getDevice()));
		}
	}

	@Test
	public void testGenerateRbConfig() throws UnknownHostException {

		for (final Station station : stationService.listAllStations()) {
			log.info(networkDeviceManagementBean.generateConfig(station.getDevice()));
		}
		// final NetworkDevice d1 = testHelperBean.createStationWithDevice("3B050205B659", "d4ca6dd444f3", "Berg", true);
		// final NetworkDevice d2 = testHelperBean.createStationWithDevice("3B05027CF736", "d4ca6db5e9e7", "Chalchegg", false);
		// final Connection connection = connectionService.connectStations(d1.getStation(), d2.getStation());
		// addressManagementBean.fillStation(d2.getStation());
		// addressManagementBean.fillStation(d1.getStation());
		// log.info(networkDeviceManagementBean.generateConfig(stationService.findStation(d1.getStation().getId()).getDevice()));
		// log.info(networkDeviceManagementBean.generateConfig(stationService.findStation(d2.getStation().getId()).getDevice()));
		// networkDeviceManagementBean.loadConfig(device, InetAddress.getByName("192.168.88.1"));
	}

}
