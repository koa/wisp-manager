package ch.bergturbenthal.wisp.manager.model.devices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum NetworkDeviceModel {
	NANO_BRIDGE_M5(new MacAddressIncrementorFactory(2, 0x10000), NetworkOperatingSystem.UBIQUITY_AIR_OS, NetworkDeviceType.WLAN, Arrays.asList(	NetworkInterfaceType.WLAN,
																																																																							NetworkInterfaceType.LAN)),
	RB750GL(new MacAddressIncrementorFactory(5, 1), NetworkOperatingSystem.MIKROTIK_ROUTER_OS, NetworkDeviceType.STATION, Arrays.asList(NetworkInterfaceType.LAN,
																																																																			NetworkInterfaceType.LAN,
																																																																			NetworkInterfaceType.LAN,
																																																																			NetworkInterfaceType.LAN,
																																																																			NetworkInterfaceType.LAN));
	public static final NetworkDeviceModel[] stationModels;
	static {
		final Map<NetworkDeviceType, Collection<NetworkDeviceModel>> modelsPerType = new HashMap<NetworkDeviceType, Collection<NetworkDeviceModel>>();
		for (final NetworkDeviceType type : NetworkDeviceType.values()) {
			modelsPerType.put(type, new ArrayList<NetworkDeviceModel>());
		}
		for (final NetworkDeviceModel model : NetworkDeviceModel.values()) {
			modelsPerType.get(model.getDeviceType()).add(model);
		}
		stationModels = modelsPerType.get(NetworkDeviceType.STATION).toArray(new NetworkDeviceModel[0]);
	}
	@Getter
	private final MacAddressIncrementorFactory addressIncrementorFactory;
	@Getter
	private final NetworkOperatingSystem deviceOs;
	@Getter
	private final NetworkDeviceType deviceType;
	@Getter
	private final List<NetworkInterfaceType> interfaces;
}
