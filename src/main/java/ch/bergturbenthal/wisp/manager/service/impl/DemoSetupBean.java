package ch.bergturbenthal.wisp.manager.service.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.bergturbenthal.wisp.manager.model.Antenna;
import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.CustomerConnection;
import ch.bergturbenthal.wisp.manager.model.DHCPSettings;
import ch.bergturbenthal.wisp.manager.model.Domain;
import ch.bergturbenthal.wisp.manager.model.GatewaySettings;
import ch.bergturbenthal.wisp.manager.model.GatewayType;
import ch.bergturbenthal.wisp.manager.model.IpAddress;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.Password;
import ch.bergturbenthal.wisp.manager.model.PortExpose;
import ch.bergturbenthal.wisp.manager.model.Position;
import ch.bergturbenthal.wisp.manager.model.RangePair;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.VLan;
import ch.bergturbenthal.wisp.manager.model.address.IpAddressType;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceType;
import ch.bergturbenthal.wisp.manager.repository.DomainRepository;
import ch.bergturbenthal.wisp.manager.repository.NetworkDeviceRepository;
import ch.bergturbenthal.wisp.manager.repository.PasswordRepository;
import ch.bergturbenthal.wisp.manager.repository.PortExposeRepository;
import ch.bergturbenthal.wisp.manager.repository.StationRepository;
import ch.bergturbenthal.wisp.manager.repository.VLanRepository;
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
	private final AtomicInteger deviceCounter = new AtomicInteger(2);
	@Autowired
	private DomainRepository domainRepository;
	@Autowired
	private NetworkDeviceManagementService networkDeviceManagementBean;
	@Autowired
	private NetworkDeviceRepository networkDeviceRepository;
	@Autowired
	private PasswordRepository passwordRepository;
	@Autowired
	private PortExposeRepository portExposeRepository;
	@Autowired
	private StationRepository stationRepository;
	@Autowired
	private StationService stationService;
	@Autowired
	private VLanRepository vlanRepository;

	private void appendPortExpose(final VLan vlan, final int port, final String address) throws UnknownHostException {
		final PortExpose expose = new PortExpose();
		expose.setPortNumber(port);
		expose.setTargetAddress(new IpAddress(InetAddress.getByName(address)));
		expose.setVlan(vlan);
		portExposeRepository.save(expose);
		vlan.getExposion().add(expose);
	}

	private void appendVlan(final Station station, final int vlanId) {
		final Set<CustomerConnection> customerConnections;
		if (station.getCustomerConnections() == null) {
			customerConnections = new HashSet<CustomerConnection>();
		} else {
			customerConnections = station.getCustomerConnections();
		}
		if (customerConnections.isEmpty()) {
			final CustomerConnection newConnection = new CustomerConnection();
			newConnection.setStation(station);
			customerConnections.add(newConnection);
		}
		final CustomerConnection customerConnection = customerConnections.iterator().next();
		final Set<VLan> networks = customerConnection.getOwnNetworks();
		final VLan vlan = new VLan();
		vlan.setVlanId(vlanId);
		networks.add(vlan);
		vlan.setCustomerConnection(customerConnection);
	}

	private NetworkDevice createRandomDevice(final NetworkDeviceModel type) {
		final int deviceNr = deviceCounter.incrementAndGet();
		final String hexNr = Integer.toHexString(deviceNr);
		final String part = hexNr.length() == 1 ? "0" + hexNr : hexNr;
		final MessageFormat macFormat = new MessageFormat("B2:8E:{0}:FA:70:90");
		final NetworkDevice device = NetworkDevice.createDevice(type, macFormat.format(new Object[] { part }));
		device.setSerialNumber(RandomStringUtils.random(12, "0123456789abcdef"));
		return device;
	}

	private Station createStation(final String name, final Position position) {
		final Station station = stationService.addStation(position);
		final CustomerConnection customerConnection = new CustomerConnection();
		customerConnection.setStation(station);
		station.setCustomerConnections(new HashSet<CustomerConnection>(Collections.singleton(customerConnection)));
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

	private VLan createVLan(final int vlanId, final String v4Address, final String v6Address, final Long dhcpStartOffset, final Long dhcpEndOffset) {
		final VLan retVlan = new VLan();
		retVlan.setVlanId(vlanId);
		if (retVlan.getAddress() == null) {
			retVlan.setAddress(new RangePair());
		}
		if (v4Address != null) {
			addressManagementBean.setAddressManually(retVlan.getAddress(), v4Address, IpAddressType.V4);
		}
		if (v6Address != null) {
			addressManagementBean.setAddressManually(retVlan.getAddress(), v6Address, IpAddressType.V6);
		}
		if (dhcpStartOffset != null && dhcpEndOffset != null) {
			final DHCPSettings dhcpSettings = new DHCPSettings();
			dhcpSettings.setLeaseTime(TimeUnit.MINUTES.toMillis(20));
			dhcpSettings.setStartOffset(dhcpStartOffset);
			dhcpSettings.setEndOffset(dhcpEndOffset);
			retVlan.setDhcpSettings(dhcpSettings);
		}
		return retVlan;
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

	@Override
	public void initDemoData() {
		try {
			deviceCounter.set(2);
			for (final NetworkDeviceType type : NetworkDeviceType.values()) {
				final Password foundPassword = passwordRepository.findOne(type);
				if (foundPassword == null) {
					final Password newPassword = new Password();
					newPassword.setDeviceType(type);
					newPassword.setPassword(RandomStringUtils.randomAlphanumeric(6));
					passwordRepository.save(newPassword);
				}
			}
			addressManagementBean.initAddressRanges();
			if (stationService.listAllStations().isEmpty()) {

				final Domain domain;
				final Domain existingDomain = domainRepository.findByDomainName("yourdomain.local");
				if (existingDomain == null) {
					final Domain newDomain = new Domain();
					newDomain.setDomainName("yourdomain.local");
					newDomain.setDnsServers(Arrays.asList(new IpAddress(InetAddress.getByName("192.168.1.50")), new IpAddress(InetAddress.getByName("192.168.1.51"))));
					domainRepository.save(newDomain);
					domain = newDomain;
				} else {
					domain = existingDomain;
				}

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
				// for (final CustomerConnection customerConnection : stationChalchegg.getCustomerConnections()) {
				// customerConnection.setName("König");
				// for (final Iterator<VLan> iterator = customerConnection.getOwnNetworks().iterator(); iterator.hasNext();) {
				// final VLan vLan = iterator.next();
				// if (vLan.getVlanId() == 0) {
				// iterator.remove();
				// }
				// }
				// }
				stationBerg.setTunnelConnection(true);
				stationBerg.setDomain(domain);
				final GatewaySettings gateway = new GatewaySettings();
				gateway.setGatewayName("Cyberlink");
				gateway.setHasIPv4(true);
				gateway.setHasIPv6(true);
				gateway.setManagementAddress(new RangePair());
				addressManagementBean.setAddressManually(gateway.getManagementAddress(), "172.28.0.2/30", IpAddressType.V4);
				gateway.setStation(stationBerg);
				gateway.setGatewayType(GatewayType.PPPOE);
				gateway.setUserName("pppoe-user");
				gateway.setPassword("pppoe-password");
				stationBerg.getGatewaySettings().add(gateway);
				final CustomerConnection customerConnection = stationBerg.getCustomerConnections().iterator().next();
				vlanRepository.delete(customerConnection.getOwnNetworks());
				customerConnection.getOwnNetworks().clear();
				final VLan normalVlan = createVLan(1, "10.14.10.1/16", "2001:1620:bba::10:1/16", Long.valueOf(256 * 60), Long.valueOf(256 * 61 - 1));
				final VLan mgmtVlan = createVLan(20, "172.30.30.2/16", null, Long.valueOf(256 * 60), Long.valueOf(256 * 61 - 1));

				appendPortExpose(normalVlan, 80, "10.14.50.31");
				appendPortExpose(normalVlan, 80, "2001:1620:bba::50:31");
				appendPortExpose(normalVlan, 443, "10.14.50.31");
				appendPortExpose(normalVlan, 443, "2001:1620:bba::50:31");
				appendPortExpose(normalVlan, 22, "10.14.50.29");
				appendPortExpose(normalVlan, 22, "2001:1620:bba::50:29");

				normalVlan.setCustomerConnection(customerConnection);
				vlanRepository.save(normalVlan);
				customerConnection.getOwnNetworks().add(normalVlan);

				mgmtVlan.setCustomerConnection(customerConnection);
				vlanRepository.save(mgmtVlan);
				customerConnection.getOwnNetworks().add(mgmtVlan);

				stationService.fillStation(stationBerg);

				stationRepository.save(Arrays.asList(stationBerg, stationChalchegg, stationFaesigrund, stationSusanne));

				// addressManagementBean.fillStation(stationChalchegg);
				// addressManagementBean.fillStation(stationSusanne);
				// addressManagementBean.fillStation(stationFaesigrund);
				// addressManagementBean.fillConnection(connection1);
				// addressManagementBean.fillConnection(connection2);
				// addressManagementBean.fillConnection(connection3);
			}
		} catch (final UnknownHostException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
