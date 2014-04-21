package ch.bergturbenthal.wisp.manager.service;

import ch.bergturbenthal.wisp.manager.model.Antenna;
import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;

public interface ConnectionService {
	Iterable<Connection> listAllConnections();

	Connection connectStations(final Station s1, final Station s2);

	CrudRepositoryContainer<Connection, Long> createConnectionContainer();

	CrudRepositoryContainer<Antenna, Long> createAntennaContainer();

	void setBridgeCount(final Connection connection, final int count);

	void fillConnection(final Connection connection);
}
