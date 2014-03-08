package ch.bergturbenthal.wisp.manager.model;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ch.bergturbenthal.wisp.manager.model.devices.MacAddressIncrementorFactory;

@RequiredArgsConstructor
public enum NetworkDeviceModel {
	NANO_BRIDGE_M5(new MacAddressIncrementorFactory(2, 0x10000), NetworkOperatingSystem.UBIQUITY_AIR_OS, Arrays.asList(NetworkInterfaceType.WLAN, NetworkInterfaceType.LAN)),
	RB750GL(new MacAddressIncrementorFactory(5, 1), NetworkOperatingSystem.MIKROTIK_ROUTER_OS, Arrays.asList(	NetworkInterfaceType.LAN,
																																																						NetworkInterfaceType.LAN,
																																																						NetworkInterfaceType.LAN,
																																																						NetworkInterfaceType.LAN,
																																																						NetworkInterfaceType.LAN));
	@Getter
	private final MacAddressIncrementorFactory addressIncrementorFactory;
	@Getter
	private final NetworkOperatingSystem deviceType;
	@Getter
	private final List<NetworkInterfaceType> interfaces;
}
