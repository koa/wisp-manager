package ch.bergturbenthal.wisp.manager.view;

import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.UIScope;
import org.vaadin.spring.navigator.VaadinView;

import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.Position;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.service.ConnectionService;
import ch.bergturbenthal.wisp.manager.service.NetworkDeviceManagementService;
import ch.bergturbenthal.wisp.manager.service.StationService;
import ch.bergturbenthal.wisp.manager.util.CrudItem;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;
import ch.bergturbenthal.wisp.manager.view.component.StationEditor;
import ch.bergturbenthal.wisp.manager.view.map.GoogleMap;

import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.tapio.googlemaps.client.LatLon;
import com.vaadin.tapio.googlemaps.client.events.MapClickListener;
import com.vaadin.tapio.googlemaps.client.events.MarkerClickListener;
import com.vaadin.tapio.googlemaps.client.events.MarkerDragListener;
import com.vaadin.tapio.googlemaps.client.overlays.GoogleMapMarker;
import com.vaadin.tapio.googlemaps.client.overlays.GoogleMapPolyline;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.VerticalLayout;

@Slf4j
@VaadinView(name = MapView.VIEW_ID)
@UIScope
public class MapView extends CustomComponent implements View {
	public static final String VIEW_ID = "Map";
	@Autowired
	private ConnectionService connectionService;
	private CrudRepositoryContainer<NetworkDevice, Long> devicesContainer;
	@Autowired
	private NetworkDeviceManagementService networkDeviceManagementService;
	@Autowired
	private StationEditor stationEditor;
	@Autowired
	private StationService stationService;

	private void doIfDiscardOk(final FieldGroup fieldGroup, final Runnable runnable) {
		try {
			if (fieldGroup.isModified()) {
				fieldGroup.commit();
			}
			runnable.run();
		} catch (final CommitException e) {
			Notification.show("Cannot save station", Type.ERROR_MESSAGE);
		}
	}

	private void drawStation(final GoogleMap googleMap, final Station station) {
		final GoogleMapMarker mapMarker = new GoogleMapMarker(station.getName(), station.getPosition().getGooglePostion(), true);
		mapMarker.setId(station.getId());
		mapMarker.setCaption(station.getName());
		googleMap.addMarker(mapMarker);
	}

	@Override
	public void enter(final ViewChangeEvent event) {

		final CrudRepositoryContainer<Station, Long> stationContainer = stationService.createContainerRepository();
		devicesContainer = networkDeviceManagementService.createContainerRepository();

		final Station emptyStation = new Station();
		emptyStation.setDevice(new NetworkDevice());
		final LatLon pos = new LatLon();
		final GoogleMap googleMap = new GoogleMap(pos, null);
		googleMap.addMapClickListener(new MapClickListener() {

			@Override
			public void mapClicked(final LatLon position) {
				final Station newStation = stationService.addStation(new Position(position));
				drawStation(googleMap, newStation);
				stationEditor.setItem(stationContainer.getItem(newStation.getId()));
				stationEditor.setVisible(true);
			}
		});
		googleMap.addMarkerDragListener(new MarkerDragListener() {

			@Override
			public void markerDragged(final GoogleMapMarker draggedMarker, final LatLon oldPosition) {
				stationService.moveStation(draggedMarker.getId(), new Position(draggedMarker.getPosition()));
				redrawLines(googleMap);
			}
		});
		googleMap.addMarkerClickListener(new MarkerClickListener() {

			@Override
			public void markerClicked(final GoogleMapMarker clickedMarker) {
				stationEditor.setItem(stationContainer.getItem(Long.valueOf(clickedMarker.getId())));
				stationEditor.setVisible(true);
			}
		});
		updateMarkers(googleMap);
		googleMap.setSizeFull();

		final VerticalLayout editPanel = new VerticalLayout(stationEditor);
		editPanel.addComponent(new Button("remove Station", new ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				final CrudItem<Station> currentItem = stationEditor.getCurrentItem();
				if (currentItem == null) {
					return;
				}
				stationService.removeStation(currentItem.getPojo());
				updateMarkers(googleMap);
				stationEditor.setVisible(false);
			}
		}));
		editPanel.addComponent(new Button("zoom all", new ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				zoomAll(googleMap);
			}
		}));
		final HorizontalSplitPanel splitPanel = new HorizontalSplitPanel(editPanel, googleMap);
		splitPanel.setSplitPosition(20, Unit.PERCENTAGE);
		splitPanel.setSizeFull();
		setCompositionRoot(splitPanel);
		setSizeFull();
		zoomAll(googleMap);
	}

	private void redrawLines(final GoogleMap googleMap) {
		googleMap.clearPolyLines();
		for (final Connection connection : connectionService.listAllConnections()) {
			final Station startStation = connection.getStartStation();
			if (startStation == null) {
				continue;
			}
			final Station endStation = connection.getEndStation();
			if (endStation == null) {
				continue;
			}
			final GoogleMapPolyline polyline = new GoogleMapPolyline(Arrays.asList(startStation.getPosition().getGooglePostion(), endStation.getPosition().getGooglePostion()));
			polyline.setId(connection.getId().longValue());
			polyline.setStrokeWeight(3);
			googleMap.addPolyline(polyline);
		}
	}

	private void updateMarkers(final GoogleMap googleMap) {
		googleMap.clearMarkers();
		for (final Station station : stationService.listAllStations()) {
			drawStation(googleMap, station);
		}
		redrawLines(googleMap);
	}

	private void zoomAll(final GoogleMap googleMap) {
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
		}
		if (ne.getLat() != Double.MIN_VALUE) {
			googleMap.fitToBounds(ne, sw);
		}
	}

}
