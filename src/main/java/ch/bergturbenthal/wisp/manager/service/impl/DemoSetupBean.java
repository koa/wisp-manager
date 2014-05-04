package ch.bergturbenthal.wisp.manager.service.impl;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.bergturbenthal.wisp.manager.model.Antenna;
import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.Position;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.VLan;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.repository.NetworkDeviceRepository;
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
	private NetworkDeviceRepository networkDeviceRepository;
	@Autowired
	private StationService stationService;

	private void appendVlan(final Station station, final int vlanId) {
		final Set<VLan> networks = station.getOwnNetworks();
		final VLan vlan = new VLan();
		vlan.setVlanId(vlanId);
		networks.add(vlan);
		vlan.setStation(station);
	}

	private NetworkDevice createRandomDevice(final NetworkDeviceModel type) {
		final NetworkDevice device = NetworkDevice.createDevice(type, RandomStringUtils.random(12, "0123456789abcdef"));
		device.setSerialNumber(RandomStringUtils.random(12, "0123456789abcdef"));
		return device;
	}

	private Station createStation(final String name, final Position position) {
		final Station station = stationService.addStation(position);
		station.setName(name);
		return station;
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

	@Override
	public void fillDummyDevice(final Antenna antenna) {
		if (antenna.getDevice() != null) {
			return;
		}
		final NetworkDevice device = createRandomDevice(NetworkDeviceModel.NANO_BRIDGE_M5);
		device.setProperties(Collections.singletonMap("radio.1.subsystemid", "dummy-id"));
		device.setAntenna(antenna);
		antenna.setDevice(networkDeviceRepository.save(device));
	}

	@Override
	public void fillDummyDevice(final Station station) {
		if (station.getDevice() != null) {
			return;
		}
		final NetworkDevice device = createRandomDevice(NetworkDeviceModel.RB750GL);
		device.setStation(station);
		station.setDevice(networkDeviceRepository.save(device));
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
			final Station stationBerg = createStation("Berg", new Position(47.4196598, 8.8859171));
			final Station stationChalchegg = createStation("Chalchegg", new Position(47.4113853, 8.9027828));
			final Station stationSusanne = createStation("Susanne", new Position(47.4186617, 8.8852251));
			final Station stationFaesigrund = createStation("Fäsigrund", new Position(47.4273742, 8.9079165));

			// final IpRange bergRootRange = addressManagementBean.addRootRange(InetAddress.getByName("10.14.0.0"), 16, 16, "Reservation König Berg");
			// final IpRange bergUserRange = addressManagementBean.reserveRange(bergRootRange, AddressRangeType.USER, 32, "Internal Berg");
			// final IpRange routerIp = addressManagementBean.reserveRange(bergUserRange, AddressRangeType.ASSIGNED, 32, "Router Address");

			// final Set<VLan> bergNetworks = stationBerg.getOwnNetworks();
			// final VLan vlan1Berg = new VLan();
			// vlan1Berg.setVlanId(1);
			// vlan1Berg.setAddress(new RangePair(bergUserRange, null));
			// bergNetworks.add(vlan1Berg);
			// vlan1Berg.setStation(stationBerg);

			final Connection connection1 = connectionService.connectStations(stationBerg, stationChalchegg);
			final Connection connection2 = connectionService.connectStations(stationSusanne, stationFaesigrund);
			final Connection connection3 = connectionService.connectStations(stationBerg, stationSusanne);

			appendVlan(stationChalchegg, 1);
			appendVlan(stationChalchegg, 2);
			appendVlan(stationChalchegg, 10);

			addressManagementBean.fillStation(stationBerg);
			// addressManagementBean.fillStation(stationChalchegg);
			// addressManagementBean.fillStation(stationSusanne);
			// addressManagementBean.fillStation(stationFaesigrund);
			// addressManagementBean.fillConnection(connection1);
			// addressManagementBean.fillConnection(connection2);
			// addressManagementBean.fillConnection(connection3);
		}
	}
}
