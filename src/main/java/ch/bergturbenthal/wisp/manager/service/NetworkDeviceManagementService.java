package ch.bergturbenthal.wisp.manager.service;

import java.net.InetAddress;

import ch.bergturbenthal.wisp.manager.model.NetworkDevice;

public interface NetworkDeviceManagementService {

	public abstract NetworkDevice detectNetworkDevice(InetAddress host);

	public abstract String generateConfig(NetworkDevice device);

	public abstract void loadConfig(InetAddress host);

}