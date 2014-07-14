package ch.bergturbenthal.wisp.manager.service;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.bergturbenthal.wisp.manager.model.CustomerConnection;
import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.Position;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.address.AddressRangeType;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.repository.AntennaRepository;
import ch.bergturbenthal.wisp.manager.repository.BridgeRepository;
import ch.bergturbenthal.wisp.manager.repository.ConnectionRepository;
import ch.bergturbenthal.wisp.manager.repository.DnsServerRepository;
import ch.bergturbenthal.wisp.manager.repository.IpRangeRepository;
import ch.bergturbenthal.wisp.manager.repository.StationRepository;
import ch.bergturbenthal.wisp.manager.repository.VLanRepository;

@Component
public class TestHelperBean {
	@Autowired
	private AddressManagementService addressManagementBean;
	@Autowired
	private AntennaRepository antennaRepository;
	@Autowired
	private BridgeRepository bridgeRepository;
	@Autowired
	private ConnectionRepository connectionRepository;
	@Autowired
	private DnsServerRepository dnsServerRepository;
	@Autowired
	private IpRangeRepository ipRangeRepository;
	@Autowired
	private StationRepository stationRepository;
	@Autowired
	private StationService stationService;
	@Autowired
	private VLanRepository vLanRepository;

	@javax.transaction.Transactional
	public void clearData() {
		stationRepository.deleteAll();
		connectionRepository.deleteAll();
		antennaRepository.deleteAll();
		bridgeRepository.deleteAll();
		// ipRangeRepository.deleteAll();
		dnsServerRepository.deleteAll();
		vLanRepository.deleteAll();
		ipRangeRepository.delete(ipRangeRepository.findAllRootRanges());
	}

	public NetworkDevice createStationWithDevice(final String serial, final String macAddress, final String name) {
		final Station station = stationService.addStation(new Position(47.4212786, 8.8859975));
		final NetworkDevice device = NetworkDevice.createDevice(NetworkDeviceModel.RB750GL, macAddress);
		station.setDevice(device);
		station.setName(name);
		station.setCustomerConnections(new HashSet<CustomerConnection>(Collections.singleton(new CustomerConnection())));
		device.setSerialNumber(serial);
		device.setStation(station);
		stationService.updateStation(station);
		return device;
	}

	public void initAddressRanges() {

		try {
			final IpRange rootV4 = addressManagementBean.addRootRange(Inet4Address.getByName("172.16.0.0"), 12, 16, "Internal v4 Range");
			final IpRange smallV4Ranges = addressManagementBean.reserveRange(rootV4, AddressRangeType.ADMINISTRATIVE, 24, null);
			addressManagementBean.reserveRange(smallV4Ranges, AddressRangeType.LOOPBACK, 32, null);
			addressManagementBean.reserveRange(smallV4Ranges, AddressRangeType.CONNECTION, 29, null);
			addressManagementBean.reserveRange(rootV4, AddressRangeType.USER, 24, null);

			final IpRange ipV6ReservationRange = addressManagementBean.addRootRange(Inet6Address.getByName("fd7e:907d:34ab::"), 48, 56, "Internal v6 Range");
			final IpRange singleRanges = addressManagementBean.reserveRange(ipV6ReservationRange, AddressRangeType.ADMINISTRATIVE, 64, "Ranges for single addresses");
			addressManagementBean.reserveRange(singleRanges, AddressRangeType.LOOPBACK, 128, null);
			addressManagementBean.reserveRange(ipV6ReservationRange, AddressRangeType.CONNECTION, 64, null);
			addressManagementBean.reserveRange(ipV6ReservationRange, AddressRangeType.USER, 64, null);
		} catch (final UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}
}
