package ch.bergturbenthal.wisp.manager.view;

import javax.ejb.EJB;

import ch.bergturbenthal.wisp.manager.model.Position;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.service.StationService;

import com.vaadin.cdi.CDIView;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.tapio.googlemaps.GoogleMap;
import com.vaadin.tapio.googlemaps.client.LatLon;
import com.vaadin.tapio.googlemaps.client.events.MapClickListener;
import com.vaadin.tapio.googlemaps.client.events.MarkerClickListener;
import com.vaadin.tapio.googlemaps.client.events.MarkerDragListener;
import com.vaadin.tapio.googlemaps.client.overlays.GoogleMapMarker;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.VerticalLayout;

@CDIView(value = MapView.VIEW_ID)
public class MapView extends CustomComponent implements View {

	public static final String VIEW_ID = "Map";
	@EJB
	private StationService stationService;

	private void drawStation(final GoogleMap googleMap, final Station station) {
		final GoogleMapMarker mapMarker = new GoogleMapMarker(station.getName(), station.getPosition().getGooglePostion(), true);
		mapMarker.setId(station.getId());
		googleMap.addMarker(mapMarker);
	}

	@Override
	public void enter(final ViewChangeEvent event) {
		final LatLon pos = new LatLon();
		// "AIzaSyAIvCue1yTaCPV4Qb8dk4lANzwkMNndNo0"
		final GoogleMap googleMap = new GoogleMap(pos, null);
		googleMap.addMapClickListener(new MapClickListener() {

			@Override
			public void mapClicked(final LatLon position) {
				drawStation(googleMap, stationService.addStation(new Position(position)));
			}
		});
		googleMap.addMarkerDragListener(new MarkerDragListener() {

			@Override
			public void markerDragged(final GoogleMapMarker draggedMarker, final LatLon oldPosition) {
				stationService.moveStation(draggedMarker.getId(), new Position(draggedMarker.getPosition()));
			}
		});
		googleMap.addMarkerClickListener(new MarkerClickListener() {

			@Override
			public void markerClicked(final GoogleMapMarker clickedMarker) {
				// TODO Auto-generated method stub

			}
		});
		final LatLon ne = new LatLon(Double.MIN_VALUE, Double.MIN_VALUE);
		final LatLon sw = new LatLon(Double.MAX_VALUE, Double.MAX_VALUE);
		for (final Station station : stationService.listAllStations()) {
			final double lat = station.getPosition().getLat();
			final double lon = station.getPosition().getLon();
			if (lat > ne.getLat()) {
				ne.setLat(lat);
			}
			if (lat < sw.getLat()) {
				sw.setLat(lat);
			}
			if (lon > ne.getLon()) {
				ne.setLon(lon);
			}
			if (lon < sw.getLon()) {
				sw.setLon(lon);
			}
			drawStation(googleMap, station);
		}
		if (ne.getLat() != Double.MIN_VALUE) {
			googleMap.fitToBounds(ne, sw);
		}
		googleMap.setSizeFull();
		final VerticalLayout editPanel = new VerticalLayout();
		final HorizontalSplitPanel splitPanel = new HorizontalSplitPanel(editPanel, googleMap);
		setCompositionRoot(splitPanel);
		setSizeFull();
	}

}
