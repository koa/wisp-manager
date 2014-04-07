package ch.bergturbenthal.wisp.manager.service.provision.routeros;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URL;
import java.text.Format;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Cleanup;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Builder;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.bergturbenthal.wisp.manager.model.IpAddress;
import ch.bergturbenthal.wisp.manager.model.MacAddress;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.NetworkInterface;
import ch.bergturbenthal.wisp.manager.model.NetworkInterfaceRole;
import ch.bergturbenthal.wisp.manager.model.RangePair;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.VLan;
import ch.bergturbenthal.wisp.manager.model.devices.DetectedDevice;
import ch.bergturbenthal.wisp.manager.model.devices.DetectedDevice.DetectedDeviceBuilder;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.service.provision.FirmwareCache;
import ch.bergturbenthal.wisp.manager.service.provision.routeros.ProvisionRouterOs.ProvisionNetworkInterface.ProvisionNetworkInterfaceBuilder;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

@Slf4j
@Component
public class ProvisionRouterOs {
	@Data
	@Builder
	public static class ProvisionNetworkInterface {
		private final String ifName;
		private final String macAddress;
		private final String parentIfName;
		private final NetworkInterfaceRole role;
		private final String v4Address;
		private final int v4Mask;
		private final String v4NetAddress;
		private final String v6Address;
		private final int v6Mask;
		private final String v6NetAddress;
		private final int vlanId;
	}

	private static String CURRENT_OS_VERSION = "6.10";

	private static Set<String> neededPackages = new HashSet<>(Arrays.asList("security", "ipv6", "system", "dhcp", "routing", "ppp"));
	private static Format OS_DOWNLOAD_URL = new MessageFormat("http://download2.mikrotik.com/routeros/{0}/routeros-{1}-{0}.npk");
	private static final String ROUTEROS_PACKAGE_PREFIX = "routeros-";

	private static final Pattern VALID_CHARS_PATTERNS = Pattern.compile("[A-Za-z0-9]+");
	static {
		Velocity.setProperty("resource.loader", "class");
		Velocity.setProperty("class.resource.loader.class", ClasspathResourceLoader.class.getName());
		Velocity.setProperty("runtime.references.strict", "true");
		Velocity.init();
	}

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

	@Setter
	@Autowired
	private FirmwareCache fwCache;
	private final JSch jSch = new JSch();

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

	public String generateConfig(final NetworkDevice device) {
		final Station station = device.getStation();
		final ArrayList<ProvisionNetworkInterface> networkInterfaces = new ArrayList<>();
		final Collection<String> existingNames = new HashSet<String>();
		for (final NetworkInterface netIf : device.getInterfaces()) {
			final String interfaceName = netIf.getInterfaceName();
			final String ifName;
			if (interfaceName == null) {
				ifName = uniqifyName(existingNames, "unassigned" + (networkInterfaces.size() + 1));
			} else {
				ifName = uniqifyName(existingNames, stripInterfaceName(interfaceName));
			}
			final String macAddress = netIf.getMacAddress().getAddress().toUpperCase();
			boolean hasAddressWithoutVlan = false;
			for (final VLan network : netIf.getNetworks()) {
				if (network.getAddress() != null) {
					final ProvisionNetworkInterfaceBuilder builder = ProvisionNetworkInterface.builder();
					if (network.getVlanId() == 0) {
						// default network
						hasAddressWithoutVlan = true;
						builder.ifName(ifName);
						builder.macAddress(macAddress);
					} else {
						// additional vlan
						builder.ifName(uniqifyName(existingNames, ifName + "-" + network.getVlanId()));
						builder.vlanId(network.getVlanId());
						builder.parentIfName(ifName);
					}
					final InetAddress inet4Address = network.getAddress().getInet4Address();
					if (inet4Address != null) {
						builder.v4Address(inet4Address.getHostAddress());
						builder.v4Mask(network.getAddress().getInet4ParentMask());
						builder.v4NetAddress(network.getAddress().getV4Address().getParentRange().getRange().getAddress().getInetAddress().getHostAddress());
					}
					final InetAddress inet6Address = network.getAddress().getInet6Address();
					if (inet6Address != null) {
						builder.v6Address(inet6Address.getHostAddress());
						builder.v6Mask(network.getAddress().getInet6ParentMask());
						builder.v6NetAddress(network.getAddress().getV6Address().getParentRange().getRange().getAddress().getInetAddress().getHostAddress());
					}
					builder.role(netIf.getRole());
					networkInterfaces.add(builder.build());
				}
			}
			if (!netIf.getNetworks().isEmpty() && !hasAddressWithoutVlan) {
				final ProvisionNetworkInterfaceBuilder builder = ProvisionNetworkInterface.builder().macAddress(macAddress);
				builder.ifName(ifName);
				builder.macAddress(macAddress);
				networkInterfaces.add(builder.build());
			}
		}

		final StringBuilder dnsServerList = new StringBuilder();
		for (final IpAddress ipAddress : device.getDnsServers()) {
			if (dnsServerList.length() > 0) {
				dnsServerList.append(",");
			}
			dnsServerList.append(ipAddress.getInetAddress().getHostAddress());
		}
		final VelocityContext context = new VelocityContext();
		context.put("station", station);
		context.put("networkInterfaces", networkInterfaces);
		context.put("dnsServers", dnsServerList.toString());
		final Template template = Velocity.getTemplate("templates/routerboard.vm");
		final StringWriter stringWriter = new StringWriter();
		template.merge(context, stringWriter);
		return stringWriter.toString();
	}

	public DetectedDevice identify(final InetAddress host) {
		try {

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
					final Map<String, MacAddress> macs = new TreeMap<String, MacAddress>();
					for (final PrintLine line : executeListCmd(session, "interface ethernet print terse", 2, 4)) {
						final Map<String, String> values = line.getValues();
						macs.put(values.get("default-name"), new MacAddress(values.get("mac-address")));
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
					resultBuilder.interfaces(new ArrayList<>(macs.values()));
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

	public void loadConfig(final NetworkDevice device, final InetAddress host) {
		final DetectedDevice detectedDevice = identify(host);
		if (!detectedDevice.getSerialNumber().equals(device.getSerialNumber())) {
			throw new IllegalArgumentException("Wrong device. Expected: " + device.getSerialNumber() + ", detected: " + detectedDevice.getSerialNumber());
		}
		try {
			final File tempFile = File.createTempFile(device.getTitle(), ".rb");
			final String configContent = generateConfig(device);
			final FileWriter fileWriter = new FileWriter(tempFile);
			try {
				fileWriter.write(configContent);
			} finally {
				fileWriter.close();
			}
			final Session session = jSch.getSession("admin", host.getHostAddress());
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();
			try {
				copyToDevice(session, tempFile, "manager.auto.rsc");
				sendCmdWithoutAnswer(session, "system reset-configuration run-after-reset=manager.auto.rsc");
			} finally {
				session.disconnect();
			}
			final RangePair loopback = device.getStation().getLoopback();
			final IpAddress newAddress = loopback.getV4Address().getRange().getAddress();
			waitForReboot(newAddress.getInetAddress());
			device.setV4Address(newAddress.getInetAddress());
			device.setV6Address(loopback.getV6Address().getRange().getAddress().getInetAddress());
			device.setProvisioned();
		} catch (final IOException | JSchException e) {
			throw new RuntimeException("Cannot load config to " + host, e);
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

	private String stripInterfaceName(final String interfaceName) {
		final Map<Pattern, String> replacements = new HashMap<>();
		replacements.put(Pattern.compile("[öÖ]"), "oe");
		replacements.put(Pattern.compile("[äÄ]"), "ae");
		replacements.put(Pattern.compile("[üÜ]"), "ue");
		String name = interfaceName;
		for (final Entry<Pattern, String> replacementEntry : replacements.entrySet()) {
			name = replacementEntry.getKey().matcher(name).replaceAll(replacementEntry.getValue());
		}
		final Matcher matcher = VALID_CHARS_PATTERNS.matcher(name);
		final StringBuilder sb = new StringBuilder();
		while (matcher.find()) {
			if (sb.length() > 0) {
				sb.append("-");
			}
			sb.append(matcher.group());
		}
		return sb.toString().toLowerCase();
	}

	private String uniqifyName(final Collection<String> existingNames, final String ifName) {
		int index = 0;
		while (true) {
			final String candidate = index == 0 ? ifName : ifName + index;
			if (!existingNames.contains(candidate)) {
				existingNames.add(candidate);
				return candidate;
			}
			index += 1;
		}
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
