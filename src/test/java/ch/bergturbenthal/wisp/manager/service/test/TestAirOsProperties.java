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
import ch.bergturbenthal.wisp.manager.model.Antenna;
import ch.bergturbenthal.wisp.manager.model.Bridge;
import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.repository.ConnectionRepository;
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
public class TestAirOsProperties {
	@Autowired
	private AddressManagementService addressManagementBean;
	@Autowired
	private ConnectionRepository connectionRepository;
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
		for (final Connection connection : connectionService.listAllConnections()) {
			connectionService.setBridgeCount(connection, 1);
			for (final Bridge bridge : connection.getBridges()) {
				demoSetupService.fillDummyDevice(bridge.getApAntenna());
				demoSetupService.fillDummyDevice(bridge.getClientAntenna());
			}
		}
	}

	@Test
	public void testCreateAirOsConfig() {
		final Connection connection = connectionRepository.findAll().iterator().next();
		final Antenna apAntenna = connection.getBridges().get(0).getApAntenna();
		final String generatedConfig = networkDeviceManagementBean.generateConfig(apAntenna.getDevice());
		System.out.println(generatedConfig);
	}

}
