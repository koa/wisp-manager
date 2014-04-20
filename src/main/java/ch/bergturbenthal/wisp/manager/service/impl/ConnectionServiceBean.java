package ch.bergturbenthal.wisp.manager.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.bergturbenthal.wisp.manager.model.Antenna;
import ch.bergturbenthal.wisp.manager.model.Bridge;
import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.repository.AntennaRepository;
import ch.bergturbenthal.wisp.manager.repository.BridgeRepository;
import ch.bergturbenthal.wisp.manager.repository.ConnectionRepository;
import ch.bergturbenthal.wisp.manager.service.AddressManagementService;
import ch.bergturbenthal.wisp.manager.service.ConnectionService;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;

@Component
@Transactional
public class ConnectionServiceBean implements ConnectionService {
	@Autowired
	private AddressManagementService addressManagementService;
	@Autowired
	private AntennaRepository antennaRepository;
	@Autowired
	private BridgeRepository bridgeRepository;
	@Autowired
	private ConnectionRepository connectionRepository;

	@Override
	public Connection connectStations(final Station s1, final Station s2) {
		final Connection connection = new Connection();
		connection.setStartStation(s1);
		connection.setEndStation(s2);
		// s1.getBeginningConnections().add(connection);
		// s2.getEndingConnections().add(connection);
		return connectionRepository.save(connection);
	}

	private void fillAntenna(final Antenna antenna) {
		if (antenna.getAdminPassword() == null) {
			antenna.setAdminPassword(RandomStringUtils.randomAlphanumeric(10));
		}
	}

	@Override
	public void fillConnection(final Connection connection) {
		addressManagementService.fillConnection(connection);
		for (final Bridge bridge : connection.getBridges()) {
			if (bridge.getWpa2Key() == null) {
				bridge.setWpa2Key(RandomStringUtils.randomAlphanumeric(63));
			}
			fillAntenna(bridge.getApAntenna());
			fillAntenna(bridge.getClientAntenna());
		}
	}

	@Override
	public Iterable<Connection> listAllConnections() {
		return connectionRepository.findAll();
	}

	@Override
	public CrudRepositoryContainer<Connection, Long> makeContainer() {
		return new CrudRepositoryContainer<Connection, Long>(connectionRepository, Connection.class) {

			@Override
			protected Long idFromValue(final Connection entry) {
				return entry.getId();
			}
		};
	}

	@Override
	public void setBridgeCount(final Connection connection, final int count) {
		if (connection.getBridges() == null) {
			connection.setBridges(new ArrayList<Bridge>());
		}
		final List<Bridge> bridges = connection.getBridges();
		while (bridges.size() < count) {
			final Bridge bridge = new Bridge();
			bridge.setConnection(connection);
			final Bridge savedBridge = bridgeRepository.save(bridge);
			savedBridge.setApAntenna(antennaRepository.save(new Antenna()));
			savedBridge.setClientAntenna(antennaRepository.save(new Antenna()));
			bridges.add(savedBridge);
		}
		while (bridges.size() > count) {
			final Bridge removed = bridges.remove(bridges.size() - 1);
			bridgeRepository.delete(removed);
		}
		fillConnection(connection);
	}

}
