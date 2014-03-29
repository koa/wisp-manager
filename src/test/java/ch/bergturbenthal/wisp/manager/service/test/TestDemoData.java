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

import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.service.AddressManagementBean;
import ch.bergturbenthal.wisp.manager.service.ConnectionServiceBean;
import ch.bergturbenthal.wisp.manager.service.DemoSetupBean;
import ch.bergturbenthal.wisp.manager.service.NetworkDeviceManagementBean;
import ch.bergturbenthal.wisp.manager.service.StationService;
import ch.bergturbenthal.wisp.manager.service.StationServiceBean;
import ch.bergturbenthal.wisp.manager.service.TestHelperBean;
import ch.bergturbenthal.wisp.manager.service.provision.FirmwareCache;
import ch.bergturbenthal.wisp.manager.service.provision.routeros.ProvisionRouterOs;

@RunWith(Arquillian.class)
public class TestDemoData {
	@Deployment
	public static JavaArchive createDeployment() {
		return ShrinkWrap.create(JavaArchive.class)
											.addClass(AddressManagementBean.class)
											.addClass(DemoSetupBean.class)
											.addClass(StationServiceBean.class)
											.addClass(ConnectionServiceBean.class)
											.addClass(NetworkDeviceManagementBean.class)
											.addClass(ProvisionRouterOs.class)
											.addClass(FirmwareCache.class)
											.addClass(TestHelperBean.class)
											.addAsResource("META-INF/persistence.xml")
											.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
	}

	@EJB
	private DemoSetupBean demoSetupBean;
	@EJB
	private NetworkDeviceManagementBean networkDeviceManagementBean;
	@EJB
	private StationService stationService;
	@EJB
	private TestHelperBean testHelperBean;

	@Before
	public void initData() throws UnknownHostException {
		testHelperBean.clearData();
	}

	@Test
	public void testDemoSetup() {
		demoSetupBean.initDemoData();
		for (final Station station : stationService.listAllStations()) {
			System.out.println("--------------------------");
			System.out.println(station.getName());
			System.out.println(networkDeviceManagementBean.generateConfig(station.getDevice()));
		}
	}

}
