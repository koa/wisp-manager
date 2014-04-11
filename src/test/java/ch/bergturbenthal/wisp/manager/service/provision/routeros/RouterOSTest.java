package ch.bergturbenthal.wisp.manager.service.provision.routeros;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import lombok.extern.slf4j.Slf4j;

import org.junit.Ignore;
import org.junit.Test;

import ch.bergturbenthal.wisp.manager.model.devices.DetectedDevice;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.service.provision.FirmwareCache;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

@Slf4j
public class RouterOSTest {
	@Test
	@Ignore
	public void testCheckStatus() {
		final ProvisionRouterOs routerOs = new ProvisionRouterOs();
		routerOs.setFwCache(new FirmwareCache());
		final DetectedDevice macList = routerOs.identify(NetworkDeviceModel.RB750GL.getFactoryDefaultAddress());
		log.info(macList.toString());
	}

	@Test
	@Ignore
	public void testConnectDefault() throws JSchException, IOException {
		final JSch jSch = new JSch();
		JSch.setLogger(new SSHLogger());
		final Session session = jSch.getSession("admin", NetworkDeviceModel.RB750GL.getFactoryDefaultAddress().getHostAddress());
		final HostKeyRepository hostKeyRepository = jSch.getHostKeyRepository();
		session.setConfig("StrictHostKeyChecking", "no");
		session.connect();
		for (final HostKey hostKey : hostKeyRepository.getHostKey()) {
			log.info(hostKey.getHost() + ":" + hostKey.getKey());
		}
		final ChannelExec cmdChannel = (ChannelExec) session.openChannel("exec");
		cmdChannel.setCommand("interface ethernet print terse");
		cmdChannel.setInputStream(null);
		cmdChannel.setErrStream(System.err);
		cmdChannel.connect();
		final BufferedReader reader = new BufferedReader(new InputStreamReader(cmdChannel.getInputStream()));
		while (true) {
			final String line = reader.readLine();
			if (line == null) {
				break;
			}
			if (line.length() > 10) {
				final PrintLine parsedLine = PrintLine.parseLine(3, 3, line);
				log.info(parsedLine.toString());
				// final int interfaceNr = Integer.parseInt(line.substring(0, 3).trim());
				// final String flags = line.substring(3, 6);
				// final String[] values = line.substring(6).split(" ");
				// for (final String value : values) {
				// if (value.startsWith("mac-address=")) {
				// System.out.println(interfaceNr + ": " + value);
				// }
				// }

			}
			// System.out.println(line);
		}
		log.info("exit-status: " + cmdChannel.getExitStatus());
		cmdChannel.disconnect();
		session.disconnect();
	}
}
