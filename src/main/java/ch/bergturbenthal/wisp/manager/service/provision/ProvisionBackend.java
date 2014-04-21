package ch.bergturbenthal.wisp.manager.service.provision;

import java.net.InetAddress;
import java.util.Map;
import java.util.Set;

import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.devices.DetectedDevice;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceType;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkOperatingSystem;

public interface ProvisionBackend {

	String generateConfig(final NetworkDevice device);

	DetectedDevice identify(final InetAddress host, final Map<NetworkDeviceType, Set<String>> pwCandidates);

	void loadConfig(final NetworkDevice device, final InetAddress host);

	NetworkOperatingSystem supportedOs();

}
