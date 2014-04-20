package ch.bergturbenthal.wisp.manager.service.provision;

import java.net.InetAddress;

import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.devices.DetectedDevice;

public interface Provision {

	public abstract String generateConfig(NetworkDevice device);

	public abstract DetectedDevice identify(InetAddress host);

	public abstract void loadConfig(NetworkDevice device, InetAddress host);

}