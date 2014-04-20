package ch.bergturbenthal.wisp.manager.service.provision;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.devices.DetectedDevice;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkOperatingSystem;

@Component
public class ProvisionImpl implements Provision {
	@Autowired
	private List<ProvisionBackend> availableBackends;

	private final Map<NetworkOperatingSystem, ProvisionBackend> backendsByOs = new HashMap<NetworkOperatingSystem, ProvisionBackend>();

	@Override
	public String generateConfig(final NetworkDevice device) {
		final NetworkOperatingSystem deviceOs = device.getDeviceModel().getDeviceOs();
		return backendsByOs.get(deviceOs).generateConfig(device);
	}

	@Override
	public DetectedDevice identify(final InetAddress host) {
		for (final ProvisionBackend backend : availableBackends) {
			final DetectedDevice identified = backend.identify(host);
			if (identified != null) {
				return identified;
			}
		}
		return null;
	}

	@Override
	public void loadConfig(final NetworkDevice device, final InetAddress host) {
		final NetworkOperatingSystem deviceOs = device.getDeviceModel().getDeviceOs();
		backendsByOs.get(deviceOs).loadConfig(device, host);
	}

	@PostConstruct
	public void sortByBackend() {
		for (final ProvisionBackend backend : availableBackends) {
			backendsByOs.put(backend.supportedOs(), backend);
		}
	}

}
