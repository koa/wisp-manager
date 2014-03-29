package ch.bergturbenthal.wisp.manager.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.Position;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.VLan;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;

@Stateless
public class DemoSetupBean {
	@EJB
	private AddressManagementBean addressManagementBean;
	@EJB
	private ConnectionService connectionService;
	@EJB
	private NetworkDeviceManagementBean networkDeviceManagementBean;
	@EJB
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

	public void initDemoData() {
		addressManagementBean.initAddressRanges();
		if (stationService.listAllStations().isEmpty()) {
			try {
				final NetworkDevice d1 = createStationWithDevice("3B050205B659", "d4ca6dd444f3", "Berg", new Position(47.4202405, 8.8861531));
				final NetworkDevice d2 = createStationWithDevice("3B05027CF736", "d4ca6db5e9e7", "Chalchegg", new Position(47.4111584, 8.9025307));
				final Station stationBerg = d1.getStation();
				final Station stationChalchegg = d2.getStation();
				final Connection connection = connectionService.connectStations(stationBerg, stationChalchegg);

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
				addressManagementBean.fillConnection(connection);

				networkDeviceManagementBean.detectNetworkDevice(InetAddress.getByName("172.16.0.1"));
				networkDeviceManagementBean.detectNetworkDevice(InetAddress.getByName("172.16.0.2"));
			} catch (final UnknownHostException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
