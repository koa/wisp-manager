package ch.bergturbenthal.wisp.manager.service;

import java.util.Collection;

import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.Position;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;

public interface StationService {

	Station addStation(final Position position);

	Station moveStation(final long station, final Position newPosition);

	Station findStation(final long id);

	void updateStation(final Station station);

	Collection<Station> listAllStations();

	boolean removeStation(final Station bean);

	Iterable<Connection> findConnectionsOfStation(final long station);

	CrudRepositoryContainer<Station, Long> createContainerRepository();

	Station fillStation(final Station station);
}