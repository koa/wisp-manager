package ch.bergturbenthal.wisp.manager.service.impl;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.IpAddress;
import ch.bergturbenthal.wisp.manager.model.MacAddress;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.NetworkInterface;
import ch.bergturbenthal.wisp.manager.model.devices.DetectedDevice;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkInterfaceType;
import ch.bergturbenthal.wisp.manager.repository.NetworkDeviceRepository;
import ch.bergturbenthal.wisp.manager.service.AddressManagementService;
import ch.bergturbenthal.wisp.manager.service.ConnectionService;
import ch.bergturbenthal.wisp.manager.service.NetworkDeviceManagementService;
import ch.bergturbenthal.wisp.manager.service.StationService;
import ch.bergturbenthal.wisp.manager.service.provision.Provision;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;

@Slf4j
@Component
@Transactional
public class NetworkDeviceManagementBean implements NetworkDeviceManagementService {
	@Autowired
	private AddressManagementService addressManagementService;
	@Autowired
	private ConnectionService connectionService;
	@Autowired
	private ExecutorService executorService;
	@Autowired
	private Provision provision;
	@Autowired
	private NetworkDeviceRepository repository;
	@Autowired
	private StationService stationService;
	@Autowired
	private PlatformTransactionManager transactionManager;

	@Override
	public CrudRepositoryContainer<NetworkDevice, Long> createContainerRepository() {
		return new CrudRepositoryContainer<NetworkDevice, Long>(repository, NetworkDevice.class) {

			@Override
			protected Long idFromValue(final NetworkDevice entry) {
				return entry.getId();
			}
		};
	}

	@Override
	public void createDevice(final NetworkDeviceModel model) {
		repository.save(NetworkDevice.createDevice(model));
	}

	@Override
	public NetworkDevice detectNetworkDevice(final InetAddress host) {
		try {
			if (!host.isReachable(150)) {
				return null;
			}
		} catch (final IOException e) {
			throw new RuntimeException("Cannot ping host " + host, e);
		}
		final DetectedDevice identifiedDevice = provision.identify(host);
		if (identifiedDevice == null) {
			return null;
		}
		final List<NetworkDevice> resultList = repository.findBySerialNumber(identifiedDevice.getSerialNumber());
		if (resultList.size() > 0) {
			final NetworkDevice foundDevice = resultList.get(0);
			updateDevice(foundDevice, identifiedDevice);
			setKnownIp(foundDevice, host);
			return foundDevice;
		} else {
			final NetworkDevice newDevice = NetworkDevice.createDevice(identifiedDevice.getModel());
			updateDevice(newDevice, identifiedDevice);
			newDevice.setSerialNumber(identifiedDevice.getSerialNumber());
			setKnownIp(newDevice, host);
			repository.save(newDevice);
			return newDevice;
		}
	}

	@Override
	public void fillNetworkDevice(final NetworkDevice device) {
		if (device.getStation() != null) {
			stationService.fillStation(device.getStation());
		}
		if (device.getAntenna() != null) {
			final Connection connection = device.getAntenna().getBridge().getConnection();
			connectionService.fillConnection(connection);
			stationService.fillStation(connection.getStartStation());
			stationService.fillStation(connection.getEndStation());
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.bergturbenthal.wisp.manager.service.impl.NetworkManagementService#generateConfig(ch.bergturbenthal.wisp.manager.model.NetworkDevice)
	 */
	@Override
	public String generateConfig(final NetworkDevice device) {
		fillNetworkDevice(device);
		return provision.generateConfig(device);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.bergturbenthal.wisp.manager.service.impl.NetworkManagementService#loadConfig(java.net.InetAddress)
	 */
	@Override
	public boolean loadConfig(final InetAddress... hosts) {
		for (final InetAddress host : hosts) {
			if (host == null) {
				continue;
			}
			final NetworkDevice detectNetworkDevice = detectNetworkDevice(host);
			if (detectNetworkDevice == null) {
				continue;
			}
			log.info("Detected: " + detectNetworkDevice);
			fillNetworkDevice(detectNetworkDevice);
			provision.loadConfig(detectNetworkDevice, host);
			return true;
		}
		return false;
	}

	@Override
	public Collection<NetworkDevice> scanForDevices() {
		final Collection<Future<NetworkDevice>> futures = new ArrayList<Future<NetworkDevice>>();
		for (final InetAddress ip : addressManagementService.listPossibleNetworkDevices()) {
			futures.add(executorService.submit(new Callable<NetworkDevice>() {

				@Override
				public NetworkDevice call() throws Exception {
					return new TransactionTemplate(transactionManager).execute(new TransactionCallback<NetworkDevice>() {

						@Override
						public NetworkDevice doInTransaction(final TransactionStatus status) {
							final String hostAddress = ip.getHostAddress();
							log.info("Check device: " + hostAddress);
							return detectNetworkDevice(ip);
						}
					});
				}
			}));
		}
		final ArrayList<NetworkDevice> ret = new ArrayList<NetworkDevice>();
		for (final Future<NetworkDevice> future : futures) {
			try {
				final NetworkDevice networkDevice = future.get();
				if (networkDevice != null) {
					ret.add(networkDevice);
				}
			} catch (ExecutionException | InterruptedException e) {
				log.warn("Cannot detect a Network-Device", e);
			}
		}
		return ret;
	}

	private Collection<InetAddress> findDnsServers() {
		final ArrayList<InetAddress> ret = new ArrayList<>();
		for (final IpAddress entry : addressManagementService.listGlobalDnsServers()) {
			ret.add(entry.getInetAddress());
		}
		return ret;
	}

	private void setKnownIp(final NetworkDevice device, final InetAddress host) {
		if (host == null) {
			return;
		}
		if (host instanceof Inet4Address) {
			device.setV4Address(host);
		}
		if (host instanceof Inet6Address) {
			device.setV6Address(host);
		}

	}

	private void updateDevice(final NetworkDevice deviceEntity, final DetectedDevice identifiedDevice) {
		deviceEntity.setDeviceModel(identifiedDevice.getModel());
		final List<NetworkInterface> interfaces = deviceEntity.getInterfaces();
		final List<MacAddress> identifiedInterfaces = identifiedDevice.getInterfaces();
		for (int i = 0; i < identifiedInterfaces.size(); i++) {
			final MacAddress identifiedMacAddress = identifiedInterfaces.get(i);
			if (interfaces.size() > i) {
				interfaces.get(i).setMacAddress(identifiedMacAddress);
			} else {
				final NetworkInterface networkInterface = new NetworkInterface();
				networkInterface.setType(NetworkInterfaceType.LAN);
				networkInterface.setMacAddress(identifiedMacAddress);
				interfaces.add(networkInterface);
			}
		}
		while (interfaces.size() > identifiedInterfaces.size()) {
			interfaces.remove(interfaces.size() - 1);
		}
		if (deviceEntity.getProperties() == null) {
			deviceEntity.setProperties(identifiedDevice.getProperties());
		} else {
			deviceEntity.getProperties().clear();
			if (identifiedDevice.getProperties() != null) {
				deviceEntity.getProperties().putAll(identifiedDevice.getProperties());
			}
		}
		deviceEntity.setInterfaces(interfaces);
		deviceEntity.setCurrentPassword(identifiedDevice.getCurrentPassword());
	}
}
