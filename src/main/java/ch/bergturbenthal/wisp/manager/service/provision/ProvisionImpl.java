package ch.bergturbenthal.wisp.manager.service.provision;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.Password;
import ch.bergturbenthal.wisp.manager.model.RangePair;
import ch.bergturbenthal.wisp.manager.model.devices.DetectedDevice;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceType;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkOperatingSystem;
import ch.bergturbenthal.wisp.manager.repository.AntennaRepository;
import ch.bergturbenthal.wisp.manager.repository.NetworkDeviceRepository;
import ch.bergturbenthal.wisp.manager.repository.PasswordRepository;
import ch.bergturbenthal.wisp.manager.repository.StationRepository;

@Component
public class ProvisionImpl implements Provision {
	@Autowired
	private AntennaRepository antennaRepository;
	@Autowired
	private List<ProvisionBackend> availableBackends;
	private final Map<NetworkOperatingSystem, ProvisionBackend> backendsByOs = new HashMap<NetworkOperatingSystem, ProvisionBackend>();
	@Autowired
	private NetworkDeviceRepository networkDeviceRepository;
	@Autowired
	private PasswordRepository passwordRepository;

	@Autowired
	private StationRepository stationRepository;

	private boolean addressInRangePair(final RangePair loopback, final InetAddress host) {
		if (loopback == null) {
			return false;
		}
		final InetAddress inet4Address = loopback.getInet4Address();
		if (inet4Address != null && inet4Address.equals(host)) {
			return true;
		}
		final InetAddress inet6Address = loopback.getInet6Address();
		if (inet6Address != null && inet6Address.equals(host)) {
			return true;
		}
		return false;
	}

	private Map<NetworkDeviceType, Set<String>> collectPwCandidates(final InetAddress host) {
		final Map<NetworkDeviceType, Set<String>> pwCandidates = new HashMap<NetworkDeviceType, Set<String>>();
		final Map<NetworkDeviceType, Collection<String>> otherPasswords = new HashMap<NetworkDeviceType, Collection<String>>();
		for (final NetworkDeviceType type : NetworkDeviceType.values()) {
			pwCandidates.put(type, new LinkedHashSet<String>());
			otherPasswords.put(type, new ArrayList<String>());
		}
		for (final NetworkDevice device : networkDeviceRepository.findAll()) {
			final String currentPassword = device.getCurrentPassword();
			if (currentPassword != null && device.getDeviceModel() != null) {
				if ((device.getV4Address() != null && device.getV4Address().equals(host)) || (device.getV6Address() != null && device.getV6Address().equals(host))) {
					pwCandidates.get(device.getDeviceModel().getDeviceType()).add(currentPassword);
				} else {
					otherPasswords.get(device.getDeviceModel().getDeviceType()).add(currentPassword);
				}
			}
		}
		for (final Password password : passwordRepository.findAll()) {
			final String adminPassword = password.getPassword();
			if (adminPassword != null) {
				otherPasswords.get(NetworkDeviceType.STATION).add(adminPassword);
			}
		}
		for (final NetworkDeviceType type : NetworkDeviceType.values()) {
			pwCandidates.get(type).addAll(otherPasswords.get(type));
		}
		return pwCandidates;
	}

	@Override
	public String generateConfig(final NetworkDevice device) {
		final NetworkOperatingSystem deviceOs = device.getDeviceModel().getDeviceOs();
		return backendsByOs.get(deviceOs).generateConfig(device, getPasswordForDevice(device));
	}

	private String getPasswordForDevice(final NetworkDevice device) {
		final Password password = passwordRepository.findOne(device.getDeviceModel().getDeviceType());
		if (password == null) {
			return null;
		}
		return password.getPassword();
	}

	@Override
	public DetectedDevice identify(final InetAddress host) {
		final Map<NetworkDeviceType, Set<String>> pwCandidates = collectPwCandidates(host);
		for (final ProvisionBackend backend : availableBackends) {
			final DetectedDevice identified = backend.identify(host, pwCandidates);
			if (identified != null) {
				return identified;
			}
		}
		return null;
	}

	@Override
	public void loadConfig(final NetworkDevice device, final InetAddress host) {
		final NetworkOperatingSystem deviceOs = device.getDeviceModel().getDeviceOs();
		backendsByOs.get(deviceOs).loadConfig(device, getPasswordForDevice(device), host);
	}

	@PostConstruct
	public void sortByBackend() {
		for (final ProvisionBackend backend : availableBackends) {
			backendsByOs.put(backend.supportedOs(), backend);
		}
	}

}
