package ch.bergturbenthal.wisp.manager.service.test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.ejb.EJB;
import javax.ejb.EJBException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ch.bergturbenthal.wisp.manager.service.AddressManagementBean;
import ch.bergturbenthal.wisp.manager.service.TestHelperBean;

@RunWith(Arquillian.class)
public class AddressManagementTest {
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
	}

	@Test(expected = EJBException.class)
	public void testV4AddressOverlap() throws UnknownHostException {
		addressManagementBean.addRootRange(InetAddress.getByName("10.0.0.0"), 8, 16, "Big reservation");
		addressManagementBean.addRootRange(InetAddress.getByName("10.14.20.0"), 24, 28, "Small reservation");
	}

	@Test
	public void testV4NonAddressOverlap() throws UnknownHostException {
		addressManagementBean.addRootRange(InetAddress.getByName("10.13.0.0"), 16, 20, "Big reservation");
		addressManagementBean.addRootRange(InetAddress.getByName("10.14.20.0"), 24, 28, "Small reservation");
	}

	@Test(expected = EJBException.class)
	public void testV6AddressOverlap() throws UnknownHostException {
		addressManagementBean.addRootRange(InetAddress.getByName("fd7e:907d:34ab::"), 48, 64, "Big reservation");
		addressManagementBean.addRootRange(InetAddress.getByName("fd7e:907d:34ab:200::"), 56, 64, "Small reservation");
	}

	@Test
	public void testV6NonAddressOverlap() throws UnknownHostException {
		addressManagementBean.addRootRange(InetAddress.getByName("fd7e:907d:34ab::"), 56, 64, "Big reservation");
		addressManagementBean.addRootRange(InetAddress.getByName("fd7e:907d:34ab:200::"), 56, 64, "Small reservation");
	}
}
