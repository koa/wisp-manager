package ch.bergturbenthal.wisp.manager.model.test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Assert;
import org.junit.Test;

import ch.bergturbenthal.wisp.manager.model.IpAddress;
import ch.bergturbenthal.wisp.manager.model.IpNetwork;

public class IpAddressTest {
	@Test
	public void testV4CorrectNetAddress() throws UnknownHostException {
		final IpAddress ipAddress = new IpAddress(InetAddress.getByName("192.168.1.0"));
		final IpNetwork ipNetwork = new IpNetwork(ipAddress, 24);
		Assert.assertSame(ipAddress, ipNetwork.getAddress());
	}

	@Test
	public void testV4FixedNetAddress() throws UnknownHostException {
		final IpAddress ipAddress = new IpAddress(InetAddress.getByName("192.168.1.7"));
		final IpNetwork ipNetwork = new IpNetwork(ipAddress, 24);
		Assert.assertEquals(InetAddress.getByName("192.168.1.0"), ipNetwork.getAddress().getInetAddress());
	}

	@Test
	public void testV4FixedNetAddress2() throws UnknownHostException {
		final IpAddress ipAddress = new IpAddress(InetAddress.getByName("192.168.1.7"));
		final IpNetwork ipNetwork = new IpNetwork(ipAddress, 23);
		Assert.assertEquals(InetAddress.getByName("192.168.0.0"), ipNetwork.getAddress().getInetAddress());
	}

	@Test
	public void testV6CorrectNetAddress() throws UnknownHostException {
		final IpAddress ipAddress = new IpAddress(InetAddress.getByName("2001:a8a:7::"));
		final IpNetwork ipNetwork = new IpNetwork(ipAddress, 48);
		Assert.assertSame(ipAddress, ipNetwork.getAddress());
	}

	@Test
	public void testV6FixedNetAddress() throws UnknownHostException {
		final IpAddress ipAddress = new IpAddress(InetAddress.getByName("2001:a8a:7::7"));
		final IpNetwork ipNetwork = new IpNetwork(ipAddress, 48);
		Assert.assertEquals(InetAddress.getByName("2001:a8a:7::"), ipNetwork.getAddress().getInetAddress());
	}

	@Test
	public void testV6FixedNetAddress2() throws UnknownHostException {
		final IpAddress ipAddress = new IpAddress(InetAddress.getByName("2001:a8a:7::7"));
		final IpNetwork ipNetwork = new IpNetwork(ipAddress, 47);
		Assert.assertEquals(InetAddress.getByName("2001:a8a:6::"), ipNetwork.getAddress().getInetAddress());
	}
}
