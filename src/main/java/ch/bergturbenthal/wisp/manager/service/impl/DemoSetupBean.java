package ch.bergturbenthal.wisp.manager.service.impl;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.Position;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.VLan;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.service.AddressManagementService;
import ch.bergturbenthal.wisp.manager.service.ConnectionService;
import ch.bergturbenthal.wisp.manager.service.DemoSetupService;
import ch.bergturbenthal.wisp.manager.service.NetworkDeviceManagementService;
import ch.bergturbenthal.wisp.manager.service.StationService;

@Component
@Transactional
public class DemoSetupBean implements DemoSetupService {
	@Autowired
	private AddressManagementService addressManagementBean;
	@Autowired
	private ConnectionService connectionService;
	@Autowired
	private NetworkDeviceManagementService networkDeviceManagementBean;
	@Autowired
	private StationService stationService;

	private void appendVlan(final Station station, final int vlanId) {
		final Set<VLan> networks = station.getOwnNetworks();
		final VLan vlan = new VLan();
		vlan.setVlanId(vlanId);
		networks.add(vlan);
		vlan.setStation(station);
	}

	private NetworkDevice createStationWithDevice(final String serial, final String macAddress, final String name, final Position position) {
		final Station station = stationService.addStation(position);
		final NetworkDevice device = NetworkDevice.createDevice(NetworkDeviceModel.RB750GL, macAddress);
		station.setDevice(device);
		station.setName(name);
		device.setSerialNumber(serial);
		device.setStation(station);
		return device;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.bergturbenthal.wisp.manager.service.impl.DemoSetupService#initDemoData()
	 */
	@Override
	public void initDemoData() {
		addressManagementBean.initAddressRanges();
		if (stationService.listAllStations().isEmpty()) {
			final NetworkDevice d1 = createStationWithDevice("3B050205B659", "d4ca6dd444f3", "Berg", new Position(47.4196598, 8.8859171));
			final NetworkDevice d2 = createStationWithDevice("3B05027CF736", "d4ca6db5e9e7", "Chalchegg", new Position(47.4113853, 8.9027828));
			final NetworkDevice d3 = createStationWithDevice("3B05027CF738", "d4ca6db5e9f7", "Susanne", new Position(47.4186617, 8.8852251));
			final NetworkDevice d4 = createStationWithDevice("3B05027CF739", "d4ca6db5e9d7", "Fäsigrund", new Position(47.4273742, 8.9079165));
			final Station stationBerg = d1.getStation();
			final Station stationChalchegg = d2.getStation();
			final Station stationSusanne = d3.getStation();
			final Station stationFaesigrund = d4.getStation();

			// final IpRange bergRootRange = addressManagementBean.addRootRange(InetAddress.getByName("10.14.0.0"), 16, 16, "Reservation König Berg");
			// final IpRange bergUserRange = addressManagementBean.reserveRange(bergRootRange, AddressRangeType.USER, 32, "Internal Berg");
			// final IpRange routerIp = addressManagementBean.reserveRange(bergUserRange, AddressRangeType.ASSIGNED, 32, "Router Address");

			// final Set<VLan> bergNetworks = stationBerg.getOwnNetworks();
			// final VLan vlan1Berg = new VLan();
			// vlan1Berg.setVlanId(1);
			// vlan1Berg.setAddress(new RangePair(bergUserRange, null));
			// bergNetworks.add(vlan1Berg);
			// vlan1Berg.setStation(stationBerg);

			appendVlan(stationChalchegg, 1);
			appendVlan(stationChalchegg, 2);
			appendVlan(stationChalchegg, 10);

			addressManagementBean.fillStation(stationBerg);
			addressManagementBean.fillStation(stationChalchegg);
			addressManagementBean.fillStation(stationSusanne);
			addressManagementBean.fillStation(stationFaesigrund);
			addressManagementBean.fillConnection(connectionService.connectStations(stationBerg, stationChalchegg));
			addressManagementBean.fillConnection(connectionService.connectStations(stationSusanne, stationFaesigrund));
			addressManagementBean.fillConnection(connectionService.connectStations(stationBerg, stationSusanne));
		}
	}

}
