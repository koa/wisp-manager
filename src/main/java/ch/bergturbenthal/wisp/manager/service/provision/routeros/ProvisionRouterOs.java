package ch.bergturbenthal.wisp.manager.service.provision.routeros;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.text.Format;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import lombok.Cleanup;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;

import ch.bergturbenthal.wisp.manager.model.MacAddress;
import ch.bergturbenthal.wisp.manager.model.devices.DetectedDevice;
import ch.bergturbenthal.wisp.manager.model.devices.DetectedDevice.DetectedDeviceBuilder;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.service.provision.FirmwareCache;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;

@Slf4j
public class ProvisionRouterOs {
	private static String CURRENT_OS_VERSION = "6.10";
	private static Set<String> neededPackages = new HashSet<>(Arrays.asList("security", "ipv6", "system", "dhcp", "routing", "ppp"));
	private static Format OS_DOWNLOAD_URL = new MessageFormat("http://download2.mikrotik.com/routeros/{0}/routeros-{1}-{0}.npk");
	private static final String ROUTEROS_PACKAGE_PREFIX = "routeros-";

	private static int checkAck(final InputStream in) throws IOException {
		final int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0) {
			return b;
		}
		if (b == -1) {
			return b;
		}

		if (b == 1 || b == 2) {
			final StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				System.out.print(sb.toString());
			}
			if (b == 2) { // fatal error
				System.out.print(sb.toString());
			}
		}
		return b;
	}

	@Inject
	@Setter
	private FirmwareCache fwCache;

	private void copyToDevice(final Session session, final File fromFile, final String toFile) throws JSchException, IOException {
		final ChannelExec channelExec = createChannelWithCmd(session, "scp -t " + toFile);
		try {
			@Cleanup
			final OutputStream outputStream = channelExec.getOutputStream();
			@Cleanup
			final InputStream inputStream = channelExec.getInputStream();
			channelExec.connect();
			outputStream.write(("C0644 " + fromFile.length() + " " + toFile + "\n").getBytes());
			outputStream.flush();
			if (checkAck(inputStream) != 0) {
				throw new RuntimeException("Unexpected Error from device while transferring " + fromFile);
			}
			@Cleanup
			final FileInputStream fis = new FileInputStream(fromFile);
			IOUtils.copy(fis, outputStream);
			if (checkAck(inputStream) != 0) {
				throw new RuntimeException("Unexpected Error from device while transferring " + fromFile);
			}
		} finally {
			channelExec.disconnect();
		}
	}

	private ChannelExec createChannelWithCmd(final Session session, final String cmd) throws JSchException {
		final ChannelExec cmdChannel = (ChannelExec) session.openChannel("exec");
		cmdChannel.setCommand(cmd);
		cmdChannel.setErrStream(System.err);
		return cmdChannel;
	}

	private List<PrintLine> executeListCmd(final Session session, final String cmd, final int numberLength, final int flagLength) throws JSchException, IOException {
		final ChannelExec cmdChannel = sendCmd(session, cmd);

		try {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(cmdChannel.getInputStream()));
			final List<PrintLine> ret = new ArrayList<>();
			while (true) {
				final String line = reader.readLine();
				if (line == null) {
					break;
				}
				final PrintLine parsedLine = PrintLine.parseLine(numberLength, flagLength, line);
				if (parsedLine != null) {
					ret.add(parsedLine);
				}
			}
			final int exitStatus = cmdChannel.getExitStatus();
			if (exitStatus != 0) {
				throw new IOException("Wrong result code " + exitStatus + " on command " + cmd);
			}
			return ret;
		} finally {
			cmdChannel.disconnect();
		}

	}

	public DetectedDevice identify(final InetAddress host) {
		try {
			final JSch jSch = new JSch();
			JSch.setLogger(new Logger() {

				@Override
				public boolean isEnabled(final int level) {
					return false;
				}

				@Override
				public void log(final int level, final String message) {
					System.out.println("Level: " + level + ": " + message);
				}
			});
			while (true) {
				final Session session = jSch.getSession("admin", host.getHostAddress());
				session.setConfig("StrictHostKeyChecking", "no");
				session.connect();
				try {
					final Map<String, String> packageByVersion = new LinkedHashMap<>();
					final Set<String> disabledPackages = new HashSet<>();
					String osVariant = null;
					boolean hasScheduledPackage = false;
					for (final PrintLine line : executeListCmd(session, "system package print terse", 2, 3)) {
						if (line == null) {
							continue;
						}
						final Map<String, String> values = line.getValues();
						hasScheduledPackage |= values.get("scheduled").trim().length() > 2;
						final String packageName = values.get("name");
						final String version = values.get("version");
						packageByVersion.put(packageName, version);
						if (line.getFlags().contains('X')) {
							disabledPackages.add(packageName);
						}
						if (packageName.startsWith(ROUTEROS_PACKAGE_PREFIX)) {
							osVariant = packageName.substring(ROUTEROS_PACKAGE_PREFIX.length());
						}
					}
					if (osVariant == null) {
						throw new RuntimeException("Router os variant could not be identified");
					}
					final String osVersion = packageByVersion.get(ROUTEROS_PACKAGE_PREFIX + osVariant);
					if (!osVersion.equals(CURRENT_OS_VERSION)) {
						log.info("Upgrading fw from " + osVersion + " to " + CURRENT_OS_VERSION);
						final URL downloadUrl = new URL(OS_DOWNLOAD_URL.format(new String[] { CURRENT_OS_VERSION, osVariant }));
						final File cacheEntry = fwCache.getCacheEntry(downloadUrl);
						copyToDevice(session, cacheEntry, cacheEntry.getName());
						log.info("fw upload completed, reboot for update ");
						rebootAndWait(host, session);
						log.info("host rebooted");
						continue;
					}
					boolean needReboot = hasScheduledPackage;
					for (final String pkg : neededPackages) {
						final String version = packageByVersion.get(pkg);
						if (version == null) {
							throw new RuntimeException("Missing package " + pkg);
						}
						if (!CURRENT_OS_VERSION.equals(version)) {
							throw new RuntimeException("Old version of package " + pkg + ": " + version);
						}
						if (disabledPackages.contains(pkg)) {
							sendCmdWithoutAnswer(session, "system package enable " + pkg);
							needReboot = true;
						}
					}
					if (needReboot) {
						rebootAndWait(host, session);
						continue;
					}
					final List<MacAddress> macs = new ArrayList<>();
					for (final PrintLine line : executeListCmd(session, "interface ethernet print terse", 2, 4)) {
						macs.add(new MacAddress(line.getValues().get("mac-address")));
					}
					final DetectedDeviceBuilder resultBuilder = DetectedDevice.builder();
					final ChannelExec routerBoardResult = sendCmd(session, "system routerboard print");
					try {
						final BufferedReader reader = new BufferedReader(new InputStreamReader(routerBoardResult.getInputStream()));
						final Map<String, String> values = new HashMap<String, String>();
						while (true) {
							final String line = reader.readLine();
							if (line == null) {
								break;
							}
							final String[] parts = line.split(":", 2);
							if (parts.length != 2) {
								continue;
							}
							values.put(parts[0].trim(), parts[1].trim());
						}
						final String model = values.get("model");
						if ("750GL".equals(model)) {
							resultBuilder.model(NetworkDeviceModel.RB750GL);
						}
						resultBuilder.serialNumber(values.get("serial-number"));
					} finally {
						routerBoardResult.disconnect();
					}
					resultBuilder.interfaces(macs);
					return resultBuilder.build();
				} finally {
					session.disconnect();
				}
			}
		} catch (final JSchException | IOException e) {
			log.error("Cannot identify " + host.getHostAddress(), e);
			return null;
		}

	}

	private void rebootAndWait(final InetAddress host, final Session session) throws JSchException, IOException {
		sendCmdWithoutAnswer(session, "system reboot");
		waitForReboot(host);
	}

	private ChannelExec sendCmd(final Session session, final String cmd) throws JSchException {
		final ChannelExec cmdChannel = createChannelWithCmd(session, cmd);
		cmdChannel.setInputStream(null);
		cmdChannel.connect();
		return cmdChannel;
	}

	private void sendCmdWithoutAnswer(final Session session, final String cmd) throws JSchException {
		final ChannelExec channel = sendCmd(session, cmd);
		channel.disconnect();
	}

	private void waitForReboot(final InetAddress host) throws IOException {
		try {
			// wait until start of reboot
			Thread.sleep(2000);
			while (true) {
				final boolean backAgain = host.isReachable(100);
				if (backAgain) {
					break;
				}
				Thread.sleep(200);
			}
		} catch (final InterruptedException e) {
			throw new RuntimeException("waiting for " + host + " interrupted", e);
		}
	}
}
