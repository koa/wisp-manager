package ch.bergturbenthal.wisp.manager.service;

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
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<NetworkDevice> query = criteriaBuilder.createQuery(NetworkDevice.class);
		final Root<NetworkDevice> devicePath = query.from(NetworkDevice.class);
		query.where(criteriaBuilder.equal(devicePath, identifiedDevice.getSerialNumber()));
		final List<NetworkDevice> resultList = entityManager.createQuery(query).getResultList();
		if (resultList.size() > 0) {
			final NetworkDevice foundDevice = resultList.get(0);
			updateDevice(foundDevice, identifiedDevice);
			return foundDevice;
		} else {
			final NetworkDevice newDevice = NetworkDevice.createDevice(identifiedDevice.getModel());
			updateDevice(newDevice, identifiedDevice);
			newDevice.setSerialNumber(identifiedDevice.getSerialNumber());
			entityManager.persist(newDevice);
			return newDevice;
		}
	}

	public String generateConfig(final NetworkDevice device) {
		final NetworkDevice mergedDevice = entityManager.merge(device);
		if (mergedDevice.getStation() != null) {
			addressManagementBean.fillStation(mergedDevice.getStation());
		}
		return provision.generateConfig(mergedDevice);
	}

	public void loadConfig(final NetworkDevice originalDevice, final InetAddress host) {
		final NetworkDevice device = entityManager.merge(originalDevice);
		provision.loadConfig(device, host);
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
