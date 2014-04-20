package ch.bergturbenthal.wisp.manager.service.provision;

import java.net.InetAddress;

import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.devices.DetectedDevice;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkOperatingSystem;

public interface ProvisionBackend {

	String generateConfig(final NetworkDevice device);

	DetectedDevice identify(final InetAddress host);

	void loadConfig(final NetworkDevice device, final InetAddress host);

	NetworkOperatingSystem supportedOs();

}
