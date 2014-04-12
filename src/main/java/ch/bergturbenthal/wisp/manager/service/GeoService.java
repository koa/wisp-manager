package ch.bergturbenthal.wisp.manager.service;

import ch.bergturbenthal.wisp.manager.model.Position;

public interface GeoService {
	double distance(final Position p1, final Position p2);
}
