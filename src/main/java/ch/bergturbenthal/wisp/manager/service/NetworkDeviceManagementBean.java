package ch.bergturbenthal.wisp.manager.service;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import ch.bergturbenthal.wisp.manager.model.MacAddress;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.NetworkInterface;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.devices.DetectedDevice;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkInterfaceType;
import ch.bergturbenthal.wisp.manager.service.provision.routeros.ProvisionRouterOs;

@Stateless
public class NetworkDeviceManagementBean {
	@EJB
	private AddressManagementBean addressManagementBean;
	@PersistenceContext
	private EntityManager entityManager;
	@Inject
	private ProvisionRouterOs provision;

	public NetworkDevice detectNetworkDevice(final InetAddress host) {
		final DetectedDevice identifiedDevice = provision.identify(host);
		if (identifiedDevice == null) {
			return null;
		}
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<NetworkDevice> query = criteriaBuilder.createQuery(NetworkDevice.class);
		final Root<NetworkDevice> devicePath = query.from(NetworkDevice.class);
		query.where(criteriaBuilder.equal(devicePath.get("serialNumber"), identifiedDevice.getSerialNumber()));
		final List<NetworkDevice> resultList = entityManager.createQuery(query).getResultList();
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
			entityManager.persist(newDevice);
			return newDevice;
		}
	}

	public String generateConfig(final NetworkDevice device) {
		final NetworkDevice mergedDevice = entityManager.merge(device);
		if (mergedDevice.getStation() != null) {
			final Station station = addressManagementBean.fillStation(mergedDevice.getStation());
			return provision.generateConfig(station.getDevice());
		}
		return provision.generateConfig(mergedDevice);
	}

	public void loadConfig(final InetAddress host) {
		final NetworkDevice detectNetworkDevice = detectNetworkDevice(host);
		if (detectNetworkDevice == null) {
			return;
		}
		System.out.println("Detected: " + detectNetworkDevice);
		if (detectNetworkDevice.getStation() != null) {
			final Station station = addressManagementBean.fillStation(detectNetworkDevice.getStation());
			provision.loadConfig(station.getDevice(), host);
		} else {
			provision.loadConfig(detectNetworkDevice, host);
		}
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
		deviceEntity.setInterfaces(interfaces);
	}

}
