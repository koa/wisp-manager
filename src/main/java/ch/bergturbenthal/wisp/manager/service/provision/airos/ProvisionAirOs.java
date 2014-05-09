package ch.bergturbenthal.wisp.manager.service.provision.airos;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.codec.digest.Crypt;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import ch.bergturbenthal.wisp.manager.model.Antenna;
import ch.bergturbenthal.wisp.manager.model.Bridge;
import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.MacAddress;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.NetworkInterface;
import ch.bergturbenthal.wisp.manager.model.RangePair;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.VLan;
import ch.bergturbenthal.wisp.manager.model.devices.DetectedDevice;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceType;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkInterfaceType;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkOperatingSystem;
import ch.bergturbenthal.wisp.manager.service.provision.FirmwareCache;
import ch.bergturbenthal.wisp.manager.service.provision.FirmwareCache.UrlStreamProducer;
import ch.bergturbenthal.wisp.manager.service.provision.ProvisionBackend;
import ch.bergturbenthal.wisp.manager.service.provision.SSHUtil;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

@Slf4j
@Component
public class ProvisionAirOs implements ProvisionBackend {
	@Data
	private static class LoginData {
		private final String password;
		private final String username;
	}

	@Data
	private static class ModelSettings {
		private final URL downloadUrl;
		private final String lastVersion;
		private final String systemCfgVersion;
	}

	private static final Pattern VALID_CHARS_PATTERNS = Pattern.compile("[A-Za-z0-9]+");
	@Setter
	@Autowired
	private FirmwareCache fwCache;
	private final JSch jSch = new JSch();
	private final Map<NetworkDeviceModel, ModelSettings> modelSettings = new HashMap<NetworkDeviceModel, ProvisionAirOs.ModelSettings>();
	private final Map<String, NetworkDeviceModel> platformModel = new HashMap<String, NetworkDeviceModel>();

	{
		try {
			modelSettings.put(NetworkDeviceModel.NANO_BRIDGE_M5, new ModelSettings(new URL("http://www.ubnt.com/downloads/XM-v5.5.8.build20991.bin"), "v5.5.8", "65545"));
			modelSettings.put(NetworkDeviceModel.NANO_BEAM_M5, new ModelSettings(new URL("http://www.ubnt.com/downloads/XW-v5.5.9.build21734.bin"), "v5.5.9", "65545"));
			platformModel.put("NanoBridge M5", NetworkDeviceModel.NANO_BRIDGE_M5);
			platformModel.put("NanoBeamM5", NetworkDeviceModel.NANO_BEAM_M5);
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	private Session connect(final InetAddress host, final String username, final String password) throws JSchException {
		final Session session = jSch.getSession(username, host.getHostAddress());
		session.setConfig("StrictHostKeyChecking", "no");
		session.setPassword(password);
		session.connect();
		return session;
	}

	private List<String> executeCommand(final Session session, final String command) throws JSchException {
		final ChannelExec cmdChannel = SSHUtil.sendCmd(session, command);
		try {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(cmdChannel.getInputStream()));
			final ArrayList<String> lines = new ArrayList<String>();
			while (true) {
				final String line = reader.readLine();
				if (line == null) {
					break;
				}
				lines.add(line);
			}
			return lines;
		} catch (final IOException e) {
			log.error("Cannot call " + command, e);
		} finally {
			cmdChannel.disconnect();
		}
		return null;
	}

	@Override
	public String generateConfig(final NetworkDevice device) {
		try {
			final Properties settings = generateProperties(device);
			final StringWriter writer = new StringWriter();
			settings.store(writer, "");
			return writer.toString();
		} catch (final IOException e) {
			throw new RuntimeException("Cannot provision airos device " + device.getTitle(), e);
		}
	}

	private Properties generateProperties(final NetworkDevice device) throws IOException {
		final Properties settings = new Properties();
		settings.load(new ClassPathResource("templates/airos.properties").getInputStream());
		final Antenna antenna = device.getAntenna();
		settings.setProperty("users.1.password", Crypt.crypt(antenna.getAdminPassword(), RandomStringUtils.randomAlphanumeric(2)));
		settings.setProperty("users.1.name", "admin");
		settings.setProperty("users.1.status", "enabled");
		final Bridge bridge = antenna.getBridge();
		final boolean isAp = antenna.getApBridge() != null;
		final Station gwStation;
		final Connection connection = bridge.getConnection();
		if (isAp) {
			settings.setProperty("aaa.1.status", "enabled");
			settings.setProperty("aaa.status", "enabled");
			settings.setProperty("radio.1.cwm.mode", "2");
			settings.setProperty("radio.1.mode", "master");
			settings.setProperty("wpasupplicant.device.1.status", "disabled");
			settings.setProperty("wpasupplicant.status", "disabled");
			gwStation = connection.getStartStation();
		} else {
			settings.setProperty("aaa.1.status", "disabled");
			settings.setProperty("aaa.status", "disabled");
			settings.setProperty("radio.1.cwm.mode", "1");
			settings.setProperty("radio.1.mode", "managed");
			settings.setProperty("wpasupplicant.device.1.status", "enabled");
			settings.setProperty("wpasupplicant.status", "enabled");
			gwStation = connection.getEndStation();
		}
		final RangePair antennaAddress = antenna.getAddresses();

		settings.setProperty("netconf.3.ip", antennaAddress.getInet4Address().getHostAddress());
		settings.setProperty("netconf.3.netmask", antennaAddress.getV4Address().getParentRange().getRange().getNetmaskAsAddress().getHostAddress());

		for (final NetworkInterface connInter : gwStation.getDevice().getInterfaces()) {
			for (final VLan vlan : connInter.getNetworks()) {
				if (vlan.getAddress().getV4Address().getParentRange() == connection.getAddresses().getV4Address()) {
					settings.put("route.1.gateway", vlan.getAddress().getInet4Address().getHostAddress());
				}
			}
		}
		settings.setProperty("radio.1.subsystemid", device.getProperties().get("radio.1.subsystemid"));
		final String wpa2Key = bridge.getWpa2Key();
		final String ssid = stripSsid(connection.getTitle() + "_" + bridge.getBridgeIndex());
		settings.setProperty("wpasupplicant.profile.1.network.1.psk", wpa2Key);
		settings.setProperty("aaa.1.wpa.psk", wpa2Key);
		settings.setProperty("aaa.1.ssid", ssid);
		settings.setProperty("wpasupplicant.profile.1.network.1.ssid", ssid);
		settings.setProperty("wireless.1.ssid", ssid);
		return settings;
	}

	private DetectedDevice identify(final InetAddress host, final List<LoginData> availableLogins) {
		while (true) {
			try {
				Session session = null;
				String successfulPassword = null;
				for (final LoginData loginData : availableLogins) {
					try {
						final String username = loginData.getUsername();
						final String password = loginData.getPassword();
						session = connect(host, username, password);
						if (username.equals("admin")) {
							successfulPassword = password;
						}
					} catch (final JSchException e) {
						log.info("Login failed, try next password", e);
					}
				}
				if (session == null) {
					return null;
				}
				try {
					final Map<String, String> status = readMcaStatus(session);

					if (status.isEmpty()) {
						return null;
					}

					final Map<String, String> boardInfo = readBoardInfo(session);
					final String platform = status.get("platform");
					final String firmwareVersion = status.get("firmwareVersion");
					final String softwareVersion = firmwareVersion.split("\\.", 3)[2];
					final NetworkDeviceModel deviceModel = platformModel.get(platform);
					if (deviceModel == null) {
						throw new RuntimeException("Unknown Platform: " + platform);
					}
					final ModelSettings settings = modelSettings.get(deviceModel);
					if (!softwareVersion.startsWith(settings.getLastVersion())) {
						final File fwFile = fwCache.getCacheEntry(settings.getDownloadUrl(), new UrlStreamProducer() {

							@Override
							public InputStream createInputStream(final URL url) throws IOException {
								final URLConnection connection = url.openConnection();
								connection.setRequestProperty("Referer", "http://www.ubnt.com/eula/?BACK=" + url.getFile());
								return connection.getInputStream();
							}
						});
						final File remoteFile = new File("/tmp/fwupdate.bin");
						SSHUtil.copyToDevice(session, fwFile, remoteFile);
						SSHUtil.sendCmdWithoutAnswer(session, "fwupdate -m");
						Thread.sleep(TimeUnit.SECONDS.toMillis(30));
						for (int i = 0; i < 300; i++) {
							if (host.isReachable(1000)) {
								continue;
							}
						}
					}
					final List<MacAddress> interfaces = readMacs(session, deviceModel);
					final Map<String, String> deviceProperties = new HashMap<String, String>(status);
					deviceProperties.putAll(boardInfo);
					return DetectedDevice.builder()
																.interfaces(interfaces)
																.model(deviceModel)
																.serialNumber(status.get("deviceId"))
																.properties(boardInfo)
																.currentPassword(successfulPassword)
																.build();
				} finally {
					session.disconnect();
				}
			} catch (final InterruptedException e) {
				log.error("Canot identify " + host, e);
			} catch (final JSchException e) {
				log.error("Canot identify " + host, e);
			} catch (final IOException e) {
				log.error("Canot identify " + host, e);
			}
			return null;
		}
	}

	@Override
	public DetectedDevice identify(final InetAddress host, final Map<NetworkDeviceType, Set<String>> pwCandidates) {
		final List<LoginData> availableLogins = new ArrayList<LoginData>();
		availableLogins.add(new LoginData("ubnt", "ubnt"));
		if (pwCandidates != null) {
			for (final String password : pwCandidates.get(NetworkDeviceType.ANTENNA)) {
				availableLogins.add(new LoginData(password, "admin"));
			}
		}
		return identify(host, availableLogins);
	}

	@Override
	public void loadConfig(final NetworkDevice device, final InetAddress host) {
		final List<LoginData> logins = possibleLoginsOfDevice(device);
		final DetectedDevice detectedDevice = identify(host, logins);
		if (!detectedDevice.getSerialNumber().equals(device.getSerialNumber())) {
			throw new IllegalArgumentException("Wrong device. Expected: " + device.getSerialNumber() + ", detected: " + detectedDevice.getSerialNumber());
		}
		final String username;
		final String password;
		if (detectedDevice.getCurrentPassword() == null) {
			username = "ubnt";
			password = "ubnt";
		} else {
			username = "admin";
			password = detectedDevice.getCurrentPassword();
		}
		try {
			final Session session = connect(host, username, password);
			final File tempFile = File.createTempFile(device.getTitle(), ".cfg");
			final Properties properties = generateProperties(device);
			final FileOutputStream os = new FileOutputStream(tempFile);
			try {
				properties.store(os, device.getTitle());
			} finally {
				os.close();
			}
			SSHUtil.copyToDevice(session, tempFile, new File("/tmp/system.cfg"));
			SSHUtil.sendCmdWithoutAnswer(session, "cfgmtd -w");
			SSHUtil.sendCmdWithoutAnswer(session, "ubntconf");
			SSHUtil.sendCmdWithoutAnswer(session, "reboot");
			tempFile.delete();
			device.setCurrentPassword(device.getAntenna().getAdminPassword());
			device.setV4Address(device.getAntenna().getAddresses().getInet4Address());
			device.setProvisioned();

		} catch (final IOException e) {
			throw new RuntimeException("Cannot load config to " + host, e);
		} catch (final JSchException e) {
			throw new RuntimeException("Cannot load config to " + host, e);
		}

	}

	private void parseLines(final Iterator<String> linesIterator, final Map<String, String> resultMap) {
		while (linesIterator.hasNext()) {
			final String line = linesIterator.next();
			if (line == null) {
				continue;
			}
			final String trimmedLine = line.trim();
			if (trimmedLine.isEmpty()) {
				continue;
			}
			final String[] parts = trimmedLine.split("=", 2);
			if (parts.length != 2) {
				continue;
			}
			resultMap.put(parts[0], parts[1]);
		}
	}

	private List<LoginData> possibleLoginsOfDevice(final NetworkDevice device) {
		final List<LoginData> logins = new ArrayList<ProvisionAirOs.LoginData>();
		final String currentPassword = device.getCurrentPassword();
		if (currentPassword != null) {
			logins.add(new LoginData(currentPassword, "admin"));
		}
		logins.add(new LoginData("ubnt", "ubnt"));
		final String adminPassword = device.getAntenna().getAdminPassword();
		logins.add(new LoginData(adminPassword, "admin"));
		return logins;
	}

	private Map<String, String> readBoardInfo(final Session session) throws JSchException {
		final List<String> statusLines = executeCommand(session, "cat /etc/board.info");
		final Map<String, String> ret = new HashMap<String, String>();
		parseLines(statusLines.iterator(), ret);
		return ret;
	}

	private List<MacAddress> readMacs(final Session session, final NetworkDeviceModel deviceModel) throws JSchException {
		final Map<NetworkInterfaceType, Queue<Integer>> remainingIndicesOfTypes = deviceModel.findIndicesOfTypes();
		final ArrayList<MacAddress> interfaces = new ArrayList<MacAddress>();
		NetworkInterfaceType typeOfNext = null;
		for (final String line : executeCommand(session, "ip link show")) {
			if (typeOfNext == null) {
				if (line.contains("eth0")) {
					typeOfNext = NetworkInterfaceType.LAN;
				} else if (line.contains("ath0")) {
					typeOfNext = NetworkInterfaceType.WLAN;
				}
			} else {
				final String trimmedLine = line.trim();
				if (trimmedLine.startsWith("link/ether")) {
					final Queue<Integer> remainingIndices = remainingIndicesOfTypes.get(typeOfNext);
					typeOfNext = null;
					final String mac = trimmedLine.split(" ")[1];
					if (remainingIndices == null || remainingIndices.isEmpty()) {
						log.warn("No interface remaining index for mac " + mac);
						continue;
					}
					final int index = remainingIndices.poll().intValue();
					while (interfaces.size() <= index) {
						interfaces.add(null);
					}
					interfaces.set(index, new MacAddress(mac));
				}
			}
		}
		// TODO Auto-generated method stub
		return interfaces;
	}

	private Map<String, String> readMcaStatus(final Session session) throws JSchException {
		final List<String> statusLines = executeCommand(session, "mca-status");
		final Iterator<String> linesIterator = statusLines.iterator();
		final Map<String, String> status = new LinkedHashMap<String, String>();
		while (linesIterator.hasNext()) {
			final String firstLine = linesIterator.next();
			if (firstLine == null || firstLine.trim().isEmpty()) {
				continue;
			}
			for (final String firstLineEntry : firstLine.split(",")) {
				final String[] parts = firstLineEntry.split("=", 2);
				if (parts.length != 2) {
					continue;
				}
				status.put(parts[0].trim(), parts[1].trim());
			}
			break;
		}
		parseLines(linesIterator, status);
		return status;
	}

	private String stripSsid(final String string) {
		final Matcher matcher = VALID_CHARS_PATTERNS.matcher(string);
		final StringBuilder sb = new StringBuilder();
		while (matcher.find()) {
			if (sb.length() > 0) {
				sb.append("-");
			}
			sb.append(matcher.group());
		}
		return sb.toString();
	}

	@Override
	public NetworkOperatingSystem supportedOs() {
		return NetworkOperatingSystem.UBIQUITY_AIR_OS;
	}

}
