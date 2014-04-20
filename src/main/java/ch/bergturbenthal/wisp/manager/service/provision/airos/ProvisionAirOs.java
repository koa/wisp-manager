package ch.bergturbenthal.wisp.manager.service.provision.airos;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.concurrent.TimeUnit;

import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import ch.bergturbenthal.wisp.manager.model.MacAddress;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.devices.DetectedDevice;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
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
	private static class ModelSettings {
		private final URL downloadUrl;
		private final String lastVersion;
		private final String systemCfgVersion;
	}

	@Setter
	@Autowired
	private FirmwareCache fwCache;
	private final JSch jSch = new JSch();
	private final Map<NetworkDeviceModel, ModelSettings> modelSettings = new HashMap<NetworkDeviceModel, ProvisionAirOs.ModelSettings>();
	private final Map<String, NetworkDeviceModel> platformModel = new HashMap<String, NetworkDeviceModel>();
	{
		try {
			modelSettings.put(NetworkDeviceModel.NANO_BRIDGE_M5, new ModelSettings(new URL("http://www.ubnt.com/downloads/XM-v5.5.8.build20991.bin"), "v5.5.8", "65545"));
			platformModel.put("NanoBridge M5", NetworkDeviceModel.NANO_BRIDGE_M5);
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
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
		final Properties settings = new Properties();
		try {
			settings.load(new ClassPathResource("templates/airos.properties").getInputStream());
		} catch (final IOException e) {
			throw new RuntimeException("Cannot load airos template", e);
		}

		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DetectedDevice identify(final InetAddress host) {
		while (true) {
			try {
				final Session session = jSch.getSession("ubnt", host.getHostAddress());
				session.setConfig("StrictHostKeyChecking", "no");
				session.setPassword("ubnt");
				session.connect();
				try {
					final Map<String, String> status = readMcaStatus(session);

					final String platform = status.get("platform");
					final String firmwareVersion = status.get("firmwareVersion");
					final String softwareVersion = firmwareVersion.split("\\.", 3)[2];
					final NetworkDeviceModel deviceModel = platformModel.get(platform);
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
					return DetectedDevice.builder().interfaces(interfaces).model(deviceModel).serialNumber(status.get("deviceId")).build();
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
	public void loadConfig(final NetworkDevice device, final InetAddress host) {
		// TODO Auto-generated method stub

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
			status.put(parts[0], parts[1]);
		}
		return status;
	}

	@Override
	public NetworkOperatingSystem supportedOs() {
		return NetworkOperatingSystem.UBIQUITY_AIR_OS;
	}
}
