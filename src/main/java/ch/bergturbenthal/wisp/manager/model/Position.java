package ch.bergturbenthal.wisp.manager.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import com.vaadin.tapio.googlemaps.client.LatLon;

@Data
@Embeddable
@RequiredArgsConstructor
@AllArgsConstructor
public class Position {
	@Column(columnDefinition = "numeric(10,7)")
	private double lat;
	@Column(columnDefinition = "numeric(10,7)")
	private double lon;

	public Position(final LatLon googlePosition) {
		lat = googlePosition.getLat();
		lon = googlePosition.getLon();
	}

	public LatLon getGooglePostion() {
		return new LatLon(lat, lon);
	}

}
