package ch.bergturbenthal.wisp.manager.service.impl;

import org.springframework.stereotype.Component;

import ch.bergturbenthal.wisp.manager.model.Position;
import ch.bergturbenthal.wisp.manager.service.GeoService;

@Component
public class GeoServiceImpl implements GeoService {
	private static final int R = 6378137; // Radius of the earth

	private static double toRad(final double value) {
		return value * Math.PI / 180;
	}

	@Override
	public double distance(final Position p1, final Position p2) {

		final double latDistance = toRad(p2.getLat() - p1.getLat());
		final double lonDistance = toRad(p2.getLon() - p1.getLon());
		final double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
											+ Math.cos(toRad(p1.getLat()))
											* Math.cos(toRad(p2.getLat()))
											* Math.sin(lonDistance / 2)
											* Math.sin(lonDistance / 2);
		final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return R * c;
	}
}
