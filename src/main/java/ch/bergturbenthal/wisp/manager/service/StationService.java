package ch.bergturbenthal.wisp.manager.service;

import ch.bergturbenthal.wisp.manager.model.Position;
import ch.bergturbenthal.wisp.manager.model.Station;

public interface StationService {

	Station addStation(final Position position);

	Station moveStation(final long station, final Position newPosition);

	Iterable<Station> listAllStations();

}