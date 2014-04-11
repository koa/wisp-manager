package ch.bergturbenthal.wisp.manager.service;

import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.Station;

public interface ConnectionService {
	Iterable<Connection> listAllConnections();

	Connection connectStations(final Station s1, final Station s2);

}
