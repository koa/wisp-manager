package ch.bergturbenthal.wisp.manager.service;

import java.util.Collection;

import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.Position;
import ch.bergturbenthal.wisp.manager.model.Station;

public interface StationService {

	Station addStation(final Position position);

	Station moveStation(final long station, final Position newPosition);

	Station findStation(final long id);

	void updateStation(final Station station);

	Collection<Station> listAllStations();

	Collection<Connection> listAllConnections();

	void removeStation(final Station bean);

	Iterable<Connection> findConnectionsOfStation(final long station);

}