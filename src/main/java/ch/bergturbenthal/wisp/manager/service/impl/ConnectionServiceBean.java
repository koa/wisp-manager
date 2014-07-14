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

	@Override
	public CrudRepositoryContainer<Antenna, Long> createAntennaContainer() {
		return new CrudRepositoryContainer<Antenna, Long>(antennaRepository, Antenna.class) {

			@Override
			protected Long idFromValue(final Antenna entry) {
				return entry.getId();
			}
		};
	}

	@Override
	public CrudRepositoryContainer<Connection, Long> createConnectionContainer() {
		return new CrudRepositoryContainer<Connection, Long>(connectionRepository, Connection.class) {

			@Override
			protected Long idFromValue(final Connection entry) {
				return entry.getId();
			}
		};
	}

	private void fillAntenna(final Antenna antenna) {
		if (antenna.getAdminPassword() == null) {
			antenna.setAdminPassword(RandomStringUtils.randomAlphanumeric(10));
		}
	}

	@Override
	public void fillConnection(final Connection connection) {
		for (final Bridge brigde : connection.getBridges()) {
			if (brigde.getWpa2Key() == null) {
				brigde.setWpa2Key(RandomStringUtils.randomAlphanumeric(60));
			}
			fillAntenna(brigde.getApAntenna());
			fillAntenna(brigde.getClientAntenna());
		}
	}

	@Override
	public Iterable<Connection> listAllConnections() {
		return connectionRepository.findAll();
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
			final Antenna apAntenna = antennaRepository.save(new Antenna());
			apAntenna.setApBridge(savedBridge);
			savedBridge.setApAntenna(apAntenna);
			final Antenna clientAntenna = antennaRepository.save(new Antenna());
			clientAntenna.setClientBridge(savedBridge);
			savedBridge.setClientAntenna(clientAntenna);
			bridges.add(savedBridge);
		}
		while (bridges.size() > count) {
			final Bridge removed = bridges.remove(bridges.size() - 1);
			bridgeRepository.delete(removed);
		}
	}

}
