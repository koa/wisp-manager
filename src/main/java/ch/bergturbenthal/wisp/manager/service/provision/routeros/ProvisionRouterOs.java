package ch.bergturbenthal.wisp.manager.service.provision.routeros;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URL;
import java.text.Format;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Data;
import lombok.Setter;
import lombok.experimental.Builder;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.bergturbenthal.wisp.manager.model.DHCPSettings;
import ch.bergturbenthal.wisp.manager.model.GatewaySettings;
import ch.bergturbenthal.wisp.manager.model.IpAddress;
import ch.bergturbenthal.wisp.manager.model.IpIpv6Tunnel;
import ch.bergturbenthal.wisp.manager.model.IpNetwork;
import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.model.MacAddress;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.NetworkInterface;
import ch.bergturbenthal.wisp.manager.model.NetworkInterfaceRole;
import ch.bergturbenthal.wisp.manager.model.RangePair;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.VLan;
import ch.bergturbenthal.wisp.manager.model.address.IpAddressType;
import ch.bergturbenthal.wisp.manager.model.devices.DetectedDevice;
import ch.bergturbenthal.wisp.manager.model.devices.DetectedDevice.DetectedDeviceBuilder;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceType;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkOperatingSystem;
import ch.bergturbenthal.wisp.manager.repository.IpRangeRepository;
import ch.bergturbenthal.wisp.manager.service.provision.FirmwareCache;
import ch.bergturbenthal.wisp.manager.service.provision.ProvisionBackend;
import ch.bergturbenthal.wisp.manager.service.provision.SSHUtil;
import ch.bergturbenthal.wisp.manager.service.provision.routeros.ProvisionRouterOs.ProvisionNetworkInterface.ProvisionNetworkInterfaceBuilder;
import ch.bergturbenthal.wisp.manager.service.provision.routeros.ProvisionRouterOs.TunnelEndpoint.TunnelEndpointBuilder;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

@Slf4j
@Component
public class ProvisionRouterOs implements ProvisionBackend {
	@Data
	@Builder
	public static class FirewallRule {
		private final String action;
		private final String chain;
		private final String connectionState;
		private final String dstAddress;
		private final String dstPort;
		private final String inInterface;
		private final String outInterface;
		private final String protocol;
		private final String rejectWith;
		private final String srcAddresses;
		private final String toAddresses;
		private final String toPorts;

		public String formatRule() {
			final StringBuilder sb = new StringBuilder("add");
			appendField(sb, "action", action);
			appendField(sb, "chain", chain);
			appendField(sb, "in-interface", inInterface);
			appendField(sb, "connection-state", connectionState);
			appendField(sb, "reject-with", rejectWith);
			appendField(sb, "dst-address", dstAddress);
			appendField(sb, "dst-port", dstPort);
			appendField(sb, "protocol", protocol);
			appendField(sb, "to-addresses", toAddresses);
			appendField(sb, "to-ports", toPorts);
			appendField(sb, "out-interface", outInterface);
			appendField(sb, "src-address", srcAddresses);
			return sb.toString();
		}
	}

	@Data
	@Builder
	public static class PPPoEClient {
		private String ifName;
		private String name;
		private String password;
		private String userName;
	}

	@Data
	@Builder
	public static class ProvisionNetworkInterface {
		private final String dhcpLeaseTime;
		private final String dhcpRange;
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

	@Data
	@Builder
	public static class TunnelEndpoint {
		private final String ifName;
		private final String remoteAddress;
		private final String v4Address;
		private final int v4Mask;
		private final String v4NetAddress;
	}

	private static String CURRENT_OS_VERSION = "6.15";

	private static final PeriodFormatter DURATION_FORMAT = new PeriodFormatterBuilder().appendDays()
																																											.appendSuffix("d")
																																											.appendHours()
																																											.appendSuffix("h")
																																											.appendMinutes()
																																											.appendSuffix("m")
																																											.appendSeconds()
																																											.appendSuffix("s")
																																											.toFormatter();

	private static final List<String> FORWARD_FILTER_LIST = Arrays.asList("forward", "input");

	private static Set<String> neededPackages = new HashSet<>(Arrays.asList("security", "ipv6", "system", "dhcp", "routing", "ppp", "openflow"));

	private static Format OS_DOWNLOAD_URL = new MessageFormat("http://download2.mikrotik.com/routeros/{0}/routeros-{1}-{0}.npk");

	private static Format PKG_DOWNLOAD_URL = new MessageFormat("http://download2.mikrotik.com/routeros/{0}/{2}-{0}-{1}.npk");
	private static final String ROUTEROS_PACKAGE_PREFIX = "routeros-";
	private static final Pattern VALID_CHARS_PATTERNS = Pattern.compile("[A-Za-z0-9]+");
	static {
		Velocity.setProperty("resource.loader", "class");
		Velocity.setProperty("class.resource.loader.class", ClasspathResourceLoader.class.getName());
		Velocity.setProperty("runtime.references.strict", "true");
		Velocity.init();
	}

	private static void appendField(final StringBuilder sb, final String key, final String value) {
		if (value != null) {
			sb.append(" ");
			sb.append(key);
			sb.append("=");
			sb.append(value);
		}
	}

	@Autowired
	private ScheduledExecutorService executorService;

	@Setter
	@Autowired
	private FirmwareCache fwCache;
	@Setter
	@Autowired
	private IpRangeRepository ipRangeRepository;
	private final JSch jSch = new JSch();

	private TunnelEndpoint createTunnel(final IpIpv6Tunnel tunnel, final NetworkDevice tunnelPartnerDevice, final long addressIndex, final Collection<String> existingNames) {
		final Station tunnelPartnerStation = tunnelPartnerDevice.getStation();
		final TunnelEndpointBuilder builder = TunnelEndpoint.builder();
		builder.ifName(uniqifyName(existingNames, "tunnel-" + tunnelPartnerStation.getName(), 0));
		builder.remoteAddress(tunnelPartnerStation.getLoopback().getInet6Address().getHostAddress());
		final IpNetwork localRangeInTunnel = tunnel.getV4Address().getRange();
		builder.v4Address(localRangeInTunnel.getAddress().getAddressOfNetwork(addressIndex).getHostAddress());
		builder.v4Mask(localRangeInTunnel.getNetmask());
		builder.v4NetAddress(localRangeInTunnel.getAddress().getInetAddress().getHostAddress());
		final TunnelEndpoint endpoint = builder.build();
		return endpoint;
	}

	private List<PrintLine> executeListCmd(final Session session, final String cmd, final int numberLength, final int flagLength) throws JSchException, IOException {
		final ChannelExec cmdChannel = SSHUtil.sendCmd(session, cmd);

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

	@Override
	public String generateConfig(final NetworkDevice device, final String password) {
		final Station station = device.getStation();
		final List<ProvisionNetworkInterface> networkInterfaces = new ArrayList<>();
		final List<PPPoEClient> pppoeClients = new ArrayList<PPPoEClient>();
		final List<String> dhcpClientInterfaces = new ArrayList<String>();
		final List<FirewallRule> v4FilterRules = new ArrayList<FirewallRule>();
		final List<FirewallRule> v4NatRules = new ArrayList<FirewallRule>();
		final List<FirewallRule> v6FilterRules = new ArrayList<FirewallRule>();
		for (final String chain : FORWARD_FILTER_LIST) {
			// IPv6 enabled Rules
			v6FilterRules.add(FirewallRule.builder().chain(chain).connectionState("established").build());
			v6FilterRules.add(FirewallRule.builder().chain(chain).connectionState("related").build());
			v6FilterRules.add(FirewallRule.builder().chain(chain).protocol("icmpv6").build());
			// IPv4 enabled Rules
			v4FilterRules.add(FirewallRule.builder().chain(chain).connectionState("established").build());
			v4FilterRules.add(FirewallRule.builder().chain(chain).connectionState("related").build());
			v4FilterRules.add(FirewallRule.builder().chain(chain).protocol("icmp").build());
		}
		final Collection<String> existingNames = new HashSet<String>();
		for (final NetworkInterface netIf : device.getInterfaces()) {
			final String interfaceName = netIf.getInterfaceName();
			final String ifName;
			if (interfaceName == null) {
				ifName = uniqifyName(existingNames, "unassigned", 1);
			} else {
				ifName = uniqifyName(existingNames, stripInterfaceName(interfaceName), 0);
			}
			final GatewaySettings gatewaySettings = netIf.getGatewaySettings();
			if (gatewaySettings != null) {
				final String gatewayIfName;
				switch (gatewaySettings.getGatewayType()) {
				case HE:
					gatewayIfName = ifName;
					break;
				case LAN: {
					gatewayIfName = ifName;
					final ProvisionNetworkInterfaceBuilder builder = ProvisionNetworkInterface.builder()
																																										.ifName(ifName)
																																										.macAddress(netIf.getMacAddress().getAddress().toUpperCase());
					if (gatewaySettings.isHasIPv4()) {
						dhcpClientInterfaces.add(ifName);
					}
					builder.role(netIf.getRole());
					networkInterfaces.add(builder.build());
					break;
				}
				case PPPOE:
					gatewayIfName = uniqifyName(existingNames, stripInterfaceName(gatewaySettings.getGatewayName()), 0);
					final ProvisionNetworkInterfaceBuilder baseIfBuilder = ProvisionNetworkInterface.builder()
																																													.ifName(ifName)
																																													.macAddress(netIf.getMacAddress().getAddress().toUpperCase())
																																													.role(netIf.getRole());
					final RangePair manualAddress = gatewaySettings.getManagementAddress();
					if (manualAddress != null) {
						final IpNetwork parentRange = manualAddress.getV4Address().getParentRange().getRange();
						baseIfBuilder.v4Address(manualAddress.getInet4Address().getHostAddress());
						baseIfBuilder.v4Mask(parentRange.getNetmask());
						baseIfBuilder.v4NetAddress(parentRange.getAddress().getInetAddress().getHostAddress());
					}
					networkInterfaces.add(baseIfBuilder.build());

					pppoeClients.add(PPPoEClient.builder()
																			.ifName(ifName)
																			.name(gatewayIfName)
																			.userName(gatewaySettings.getUserName())
																			.password(gatewaySettings.getPassword())
																			.build());
					break;
				default:
					gatewayIfName = ifName;
					break;
				}
				for (final IpRange range : ipRangeRepository.findAllRootRanges()) {
					final IpAddress address = range.getRange().getAddress();
					if (address.getAddressType() != IpAddressType.V4) {
						continue;
					}
					final String srcAddresses = address.getInetAddress().getHostAddress() + "/" + range.getRange().getNetmask();
					v4NatRules.add(FirewallRule.builder().action("masquerade").chain("srcnat").outInterface(gatewayIfName).srcAddresses(srcAddresses).build());
				}
				v4FilterRules.add(FirewallRule.builder().action("reject").chain("input").inInterface(gatewayIfName).rejectWith("icmp-admin-prohibited").build());
				for (final String chain : FORWARD_FILTER_LIST) {
					v6FilterRules.add(FirewallRule.builder().chain(chain).action("reject").inInterface(gatewayIfName).rejectWith("icmp-admin-prohibited").build());
				}
			}
			final String macAddress = netIf.getMacAddress().getAddress().toUpperCase();
			boolean hasAddressWithoutVlan = false;
			for (final VLan network : VLan.sortVLans(netIf.getNetworks())) {
				if (network.getAddress() != null) {
					final ProvisionNetworkInterfaceBuilder builder = ProvisionNetworkInterface.builder();
					if (network.getVlanId() == 0) {
						// default network
						hasAddressWithoutVlan = true;
						builder.ifName(ifName);
						builder.macAddress(macAddress);
					} else {
						// additional vlan
						builder.ifName(uniqifyName(existingNames, ifName + "-" + network.getVlanId(), 0));
						builder.vlanId(network.getVlanId());
						builder.parentIfName(ifName);
					}
					final InetAddress inet4Address = network.getAddress().getInet4Address();
					if (inet4Address != null) {
						final IpRange v4Address = network.getAddress().getV4Address();
						final String v4AddressString;
						final int v4Mask;
						final String v4NetAddressString;
						final IpAddress v4RangeAddress;
						if (v4Address.getRange().getNetmask() == 32) {
							// address is host-address
							v4AddressString = inet4Address.getHostAddress();
							v4Mask = network.getAddress().getInet4ParentMask();
							v4RangeAddress = v4Address.getParentRange().getRange().getAddress();
							v4NetAddressString = v4RangeAddress.getInetAddress().getHostAddress();
						} else {
							// address is net-address -> select first host-address
							v4RangeAddress = v4Address.getRange().getAddress();
							v4AddressString = v4RangeAddress.getAddressOfNetwork(1).getHostAddress();
							v4Mask = v4Address.getRange().getNetmask();
							v4NetAddressString = inet4Address.getHostAddress();
							// add default dhcp range
							builder.dhcpRange(v4RangeAddress.getAddressOfNetwork(2).getHostAddress() + "-"
																+ v4RangeAddress.getAddressOfNetwork(v4Address.getAvailableReservations() - 2).getHostAddress());
							builder.dhcpLeaseTime("10m");
						}
						builder.v4Address(v4AddressString);
						builder.v4Mask(v4Mask);
						builder.v4NetAddress(v4NetAddressString);
						final DHCPSettings dhcpSettings = network.getDhcpSettings();
						if (dhcpSettings != null) {
							builder.dhcpLeaseTime(DURATION_FORMAT.print(Duration.millis(dhcpSettings.getLeaseTime().longValue()).toPeriod()));
							final long startOffset = dhcpSettings.getStartOffset().longValue();
							final long addressCount = dhcpSettings.getAddressCount().longValue();
							builder.dhcpRange(v4RangeAddress.getAddressOfNetwork(startOffset).getHostAddress() + "-"
																+ v4RangeAddress.getAddressOfNetwork(startOffset + addressCount).getHostAddress());
						}
						if (netIf.getRole() == NetworkInterfaceRole.NETWORK) {
							for (final String chain : FORWARD_FILTER_LIST) {
								v4FilterRules.add(FirewallRule.builder()
																							.action("reject")
																							.chain(chain)
																							.inInterface(ifName)
																							.rejectWith("icmp-admin-prohibited")
																							.srcAddresses("!" + v4NetAddressString + "/" + v4Mask)
																							.build());
							}
						}
					}
					final InetAddress inet6Address = network.getAddress().getInet6Address();
					if (inet6Address != null) {
						final IpRange v6Address = network.getAddress().getV6Address();
						final String v6AddressString;
						final int v6Mask;
						final String v6NetAddressString;
						if (v6Address.getRange().getNetmask() == 128) {
							// address is host-address
							v6AddressString = inet6Address.getHostAddress();
							v6Mask = network.getAddress().getInet6ParentMask();
							v6NetAddressString = v6Address.getParentRange().getRange().getAddress().getInetAddress().getHostAddress();
						} else {
							// address is net-address -> select first host-address
							v6AddressString = inet6Address.getHostAddress();
							v6Mask = v6Address.getRange().getNetmask();
							v6NetAddressString = inet6Address.getHostAddress();
						}
						builder.v6Address(v6AddressString);
						builder.v6Mask(v6Mask);
						builder.v6NetAddress(v6NetAddressString);
						if (netIf.getRole() == NetworkInterfaceRole.NETWORK) {
							for (final String chain : FORWARD_FILTER_LIST) {
								v6FilterRules.add(FirewallRule.builder()
																							.action("reject")
																							.chain(chain)
																							.inInterface(ifName)
																							.rejectWith("icmp-admin-prohibited")
																							.srcAddresses("!" + v6NetAddressString + "/" + v6Mask)
																							.build());
							}
						}
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
		final Collection<TunnelEndpoint> tunnelEndpoints = new ArrayList<ProvisionRouterOs.TunnelEndpoint>();
		for (final IpIpv6Tunnel tunnel : device.getTunnelBegins()) {
			tunnelEndpoints.add(createTunnel(tunnel, tunnel.getEndDevice(), 1, existingNames));
		}
		for (final IpIpv6Tunnel tunnel : device.getTunnelEnds()) {
			tunnelEndpoints.add(createTunnel(tunnel, tunnel.getStartDevice(), 2, existingNames));
		}
		final Set<String> dnsServers = new TreeSet<String>();
		if (device.getDnsServers() != null) {
			for (final IpAddress ipAddress : device.getDnsServers()) {
				dnsServers.add(ipAddress.getInetAddress().getHostAddress());
			}
		}
		final VelocityContext context = new VelocityContext();
		context.put("station", station);
		context.put("password", password);
		context.put("networkInterfaces", networkInterfaces);
		context.put("tunnelEndpoints", tunnelEndpoints);
		context.put("dnsServers", StringUtils.join(dnsServers, ','));
		context.put("v4FilterRules", v4FilterRules);
		context.put("v4NatRules", v4NatRules);
		context.put("v6FilterRules", v6FilterRules);
		context.put("dhcpClientInterfaces", dhcpClientInterfaces);
		context.put("pppoeClients", pppoeClients);
		context.put("d", "$");
		final Template template = Velocity.getTemplate("templates/routerboard.vm");
		final StringWriter stringWriter = new StringWriter();
		template.merge(context, stringWriter);
		return stringWriter.toString();
	}

	@Override
	public DetectedDevice identify(final InetAddress host, final Map<NetworkDeviceType, Set<String>> pwCandidates) {
		try {
			while (true) {
				Session session = null;
				String connectedPw = null;
				final Set<String> stationCandidates = new HashSet<String>(Collections.<String> singleton(null));
				stationCandidates.addAll(pwCandidates.get(NetworkDeviceType.STATION));
				for (final String pwCandidate : stationCandidates) {
					try {
						session = jSch.getSession("admin", host.getHostAddress());
						session.setConfig("StrictHostKeyChecking", "no");
						if (pwCandidate != null) {
							session.setPassword(pwCandidate);
						}
						session.connect();
						connectedPw = pwCandidate;
						break;
					} catch (final JSchException e) {
						log.trace("wrong password, try next", e);
					}
				}
				if (session == null || !session.isConnected()) {
					log.debug("Cannot login into " + host);
					return null;
				}
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
						final File cacheEntry = fwCache.getCacheEntry(new URL(OS_DOWNLOAD_URL.format(new String[] { CURRENT_OS_VERSION, osVariant })));
						SSHUtil.copyToDevice(session, cacheEntry, new File(cacheEntry.getName()));
						log.info("fw upload completed, reboot for update ");
						rebootAndWait(host, session);
						log.info("host rebooted");
						continue;
					}
					boolean needReboot = hasScheduledPackage;
					for (final String pkg : neededPackages) {
						final String version = packageByVersion.get(pkg);
						if (version == null || !CURRENT_OS_VERSION.equals(version)) {
							log.info("installing package " + pkg);
							final File cacheEntry = fwCache.getCacheEntry(new URL(PKG_DOWNLOAD_URL.format(new String[] { CURRENT_OS_VERSION, osVariant, pkg })));
							SSHUtil.copyToDevice(session, cacheEntry, new File(cacheEntry.getName()));
							needReboot = true;
						}
						if (disabledPackages.contains(pkg)) {
							SSHUtil.sendCmdWithoutAnswer(session, "system package enable " + pkg);
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
					final ChannelExec routerBoardResult = SSHUtil.sendCmd(session, "system routerboard print");
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
					resultBuilder.currentPassword(connectedPw);
					return resultBuilder.build();
				} finally {
					session.disconnect();
				}
			}
		} catch (final JSchException | IOException e) {
			log.warn("Cannot identify " + host.getHostAddress(), e);
			return null;
		}

	}

	@Override
	public void loadConfig(final NetworkDevice device, final String adminPassword, final InetAddress host) {
		final HashMap<NetworkDeviceType, Set<String>> pwCandidates = new HashMap<NetworkDeviceType, Set<String>>();
		pwCandidates.put(	NetworkDeviceType.STATION,
											device.getCurrentPassword() == null ? Collections.<String> emptySet() : Collections.singleton(device.getCurrentPassword()));
		final DetectedDevice detectedDevice = identify(host, pwCandidates);
		if (!detectedDevice.getSerialNumber().equals(device.getSerialNumber())) {
			throw new IllegalArgumentException("Wrong device. Expected: " + device.getSerialNumber() + ", detected: " + detectedDevice.getSerialNumber());
		}
		try {
			final File tempFile = File.createTempFile(device.getTitle(), ".rb");
			final String configContent = generateConfig(device, adminPassword);
			final FileWriter fileWriter = new FileWriter(tempFile);
			try {
				fileWriter.write(configContent);
			} finally {
				fileWriter.close();
			}
			final Session session = jSch.getSession("admin", host.getHostAddress());
			session.setConfig("StrictHostKeyChecking", "no");
			session.setPassword(detectedDevice.getCurrentPassword());
			session.connect();
			try {
				SSHUtil.copyToDevice(session, tempFile, new File("manager.auto.rsc"));
				SSHUtil.sendCmdWithoutAnswer(session, "system reset-configuration run-after-reset=manager.auto.rsc");
				// final Thread currentThread = Thread.currentThread();
				// final ScheduledFuture<?> timeoutFuture = executorService.schedule(new Runnable() {
				//
				// @Override
				// public void run() {
				// currentThread.interrupt();
				// }
				// }, 10, TimeUnit.SECONDS);
				// try {
				// SSHUtil.sendCmdWithoutAnswer(session, "import manager.auto.rsc");
				// } finally {
				// timeoutFuture.cancel(false);
				// }
			} finally {
				session.disconnect();
			}
			final Station station = device.getStation();
			final RangePair loopback = station.getLoopback();
			final IpAddress newAddress = loopback.getV4Address().getRange().getAddress();
			// waitForReboot(newAddress.getInetAddress());
			device.setV4Address(newAddress.getInetAddress());
			device.setV6Address(loopback.getV6Address().getRange().getAddress().getInetAddress());
			device.setCurrentPassword(adminPassword);
			device.setProvisioned();
		} catch (final IOException | JSchException e) {
			throw new RuntimeException("Cannot load config to " + host, e);
		}
	}

	private void rebootAndWait(final InetAddress host, final Session session) throws JSchException, IOException {
		SSHUtil.sendCmdWithoutAnswer(session, "system reboot");
		waitForReboot(host);
	}

	private String stripInterfaceName(final String interfaceName) {
		if (interfaceName == null || interfaceName.trim().length() == 0) {
			return "undefined-name";
		}
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

	@Override
	public NetworkOperatingSystem supportedOs() {
		return NetworkOperatingSystem.MIKROTIK_ROUTER_OS;
	}

	private String uniqifyName(final Collection<String> existingNames, final String ifName, final int startIndex) {
		int index = startIndex;
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
