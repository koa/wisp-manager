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

import ch.bergturbenthal.wisp.manager.model.Position;
import ch.bergturbenthal.wisp.manager.model.RangePair;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.VLan;
import ch.bergturbenthal.wisp.manager.service.AddressManagementBean;
import ch.bergturbenthal.wisp.manager.service.StationService;
import ch.bergturbenthal.wisp.manager.service.StationServiceBean;
import ch.bergturbenthal.wisp.manager.service.TestHelperBean;

@RunWith(Arquillian.class)
public class StationManagementTest {
	@Deployment
	public static JavaArchive createDeployment() {
		return ShrinkWrap.create(JavaArchive.class)
											.addClass(AddressManagementBean.class)
											.addClass(StationServiceBean.class)
											.addClass(TestHelperBean.class)
											.addAsResource("META-INF/persistence.xml")
											.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
	}

	@EJB
	private AddressManagementBean addressManagementBean;
	@EJB
	private StationService stationService;

	@EJB
	private TestHelperBean testHelperBean;

	@Before
	public void initData() throws UnknownHostException {
		testHelperBean.clearData();
		testHelperBean.initAddressRanges();
	}

	@Test
	public void testCreateStation() throws UnknownHostException {
		final Station station = addressManagementBean.fillStation(stationService.addStation(new Position(47.4212786, 8.8859975)));
		Assert.assertEquals(InetAddress.getByName("172.16.1.1"), station.getLoopback().getV4Address().getRange().getAddress().getInetAddress());
		Assert.assertEquals(InetAddress.getByName("fd7e:907d:34ab::"), station.getLoopback().getV6Address().getRange().getAddress().getInetAddress());
		final VLan vlan = station.getOwnNetworks().iterator().next();
		Assert.assertEquals(0, vlan.getVlanId());
		final RangePair ownAddress = vlan.getAddress();
		Assert.assertEquals(InetAddress.getByName("172.17.0.0"), ownAddress.getInet4Address());
		Assert.assertEquals(InetAddress.getByName("fd7e:907d:34ab:200::"), ownAddress.getInet6Address());
	}

}
