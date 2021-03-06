package ch.bergturbenthal.wisp.manager.service;

import java.net.InetAddress;
import java.util.Collection;

import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;

public interface NetworkDeviceManagementService {

	NetworkDevice detectNetworkDevice(final InetAddress host);

	Collection<NetworkDevice> scanForDevices();

	String generateConfig(final NetworkDevice device);

	boolean loadConfig(final InetAddress... host);

	CrudRepositoryContainer<NetworkDevice, Long> createContainerRepository();

	void createDevice(final NetworkDeviceModel model);

	void fillNetworkDevice(final NetworkDevice device);
}