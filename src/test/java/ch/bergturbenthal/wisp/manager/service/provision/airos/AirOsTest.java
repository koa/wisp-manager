package ch.bergturbenthal.wisp.manager.service.provision.airos;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.digest.Crypt;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import ch.bergturbenthal.wisp.manager.model.devices.DetectedDevice;
import ch.bergturbenthal.wisp.manager.service.provision.FirmwareCache;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Logger;

public class AirOsTest {
	@BeforeClass
	public static void initJschLogger() {
		JSch.setLogger(new Logger() {

			@Override
			public boolean isEnabled(final int level) {
				// TODO Auto-generated method stub
				return true;
			}

			@Override
			public void log(final int level, final String message) {
				System.out.println(level + ": " + message);
			}
		});

	}

	@Test
	public void testCrypt() throws NoSuchAlgorithmException {
		final String crypt = Crypt.crypt("ubnt", "Vv");
		Assert.assertEquals("VvpvCwhccFv6Q", crypt);
	}

	@Test
	@Ignore
	public void testIdentify() throws UnknownHostException {
		final ProvisionAirOs provisionAirOs = new ProvisionAirOs();
		provisionAirOs.setFwCache(new FirmwareCache());
		final DetectedDevice detectedDevice = provisionAirOs.identify(InetAddress.getByName("192.168.1.20"));
		System.out.println(detectedDevice);
	}
}
