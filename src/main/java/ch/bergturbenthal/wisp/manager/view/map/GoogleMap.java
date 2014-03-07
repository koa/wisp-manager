package ch.bergturbenthal.wisp.manager.view.map;

import com.vaadin.tapio.googlemaps.client.LatLon;

public class GoogleMap extends com.vaadin.tapio.googlemaps.GoogleMap {

	public GoogleMap(final LatLon center, final double zoom, final String apiKeyOrClientId) {
		super(center, zoom, apiKeyOrClientId);
	}

	public GoogleMap(final LatLon center, final double zoom, final String apiKeyOrClientId, final String language) {
		super(center, zoom, apiKeyOrClientId, language);
	}

	public GoogleMap(final LatLon center, final String apiKeyOrClientId) {
		super(center, apiKeyOrClientId);
	}

	public GoogleMap(final String apiKeyOrClientId) {
		super(apiKeyOrClientId);
	}

	public void clearPolyLines() {
		getState().polylines.clear();
	}

}
