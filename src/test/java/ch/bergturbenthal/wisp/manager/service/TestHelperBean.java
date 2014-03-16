package ch.bergturbenthal.wisp.manager.service;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.Position;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.address.AddressRangeType;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;

@Stateless
public class TestHelperBean {
	@EJB
	private AddressManagementBean addressManagementBean;
	@PersistenceContext
	private EntityManager entityManager;
	@EJB
	private StationService stationService;

	public void clearData() {
		clearTable(Station.class);
		clearTable(Connection.class);
		clearTable(IpRange.class);
		clearTable(NetworkDevice.class);
	}

	public <T> void clearTable(final Class<T> type) {
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<T> query = criteriaBuilder.createQuery(type);
		query.from(type);
		for (final T entry : entityManager.createQuery(query).getResultList()) {
			entityManager.remove(entry);
		}
	}

	public NetworkDevice createStationWithDevice(final String serial, final String macAddress, final String name) {
		final Station station = stationService.addStation(new Position(47.4212786, 8.8859975));
		final NetworkDevice device = NetworkDevice.createDevice(NetworkDeviceModel.RB750GL, macAddress);
		station.setDevice(device);
		station.setName(name);
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
