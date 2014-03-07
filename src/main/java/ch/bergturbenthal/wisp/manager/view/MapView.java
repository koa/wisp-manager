package ch.bergturbenthal.wisp.manager.view;

import java.util.Arrays;

import javax.ejb.EJB;

import org.vaadin.dialogs.ConfirmDialog;

import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.Position;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.service.StationService;
import ch.bergturbenthal.wisp.manager.view.map.GoogleMap;

import com.vaadin.cdi.CDIView;
import com.vaadin.data.fieldgroup.BeanFieldGroup;
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
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Notification;

@CDIView(value = MapView.VIEW_ID)
public class MapView extends CustomComponent implements View {

	public static final String VIEW_ID = "Map";
	private FormLayout editStationForm;
	@EJB
	private StationService stationService;

	private void activateStation(final BeanFieldGroup<Station> fieldGroup, final Station station) {
		fieldGroup.setItemDataSource(station);
		editStationForm.setEnabled(true);
	}

	private void doIfDiscardOk(final BeanFieldGroup<Station> fieldGroup, final Runnable runnable) {
		if (fieldGroup.isModified()) {
			ConfirmDialog.show(getUI(), "Sure to discard changes?", new ConfirmDialog.Listener() {

				@Override
				public void onClose(final ConfirmDialog dialog) {
					if (dialog.isConfirmed()) {
						runnable.run();
					}
				}
			});
		} else {
			runnable.run();
		}
	}

	private void drawStation(final GoogleMap googleMap, final Station station) {
		final GoogleMapMarker mapMarker = new GoogleMapMarker(station.getName(), station.getPosition().getGooglePostion(), true);
		mapMarker.setId(station.getId());
		googleMap.addMarker(mapMarker);
	}

	@Override
	public void enter(final ViewChangeEvent event) {
		editStationForm = new FormLayout();

		final BeanFieldGroup<Station> fieldGroup = new BeanFieldGroup<>(Station.class);
		final LatLon pos = new LatLon();
		final GoogleMap googleMap = new GoogleMap(pos, null);
		googleMap.addMapClickListener(new MapClickListener() {

			@Override
			public void mapClicked(final LatLon position) {
				doIfDiscardOk(fieldGroup, new Runnable() {

					@Override
					public void run() {
						final Station newStation = stationService.addStation(new Position(position));
						drawStation(googleMap, newStation);
						activateStation(fieldGroup, newStation);
					}
				});
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
				doIfDiscardOk(fieldGroup, new Runnable() {
					@Override
					public void run() {
						final Station station = stationService.findStation(clickedMarker.getId());
						activateStation(fieldGroup, station);
					}
				});
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
		}
		updateMarkers(googleMap);
		googleMap.setSizeFull();
		editStationForm.addComponent(fieldGroup.buildAndBind("Name", "name"));

		editStationForm.addComponent(new Button("Save", new ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				try {
					fieldGroup.commit();
					final Station bean = fieldGroup.getItemDataSource().getBean();
					stationService.updateStation(bean);
				} catch (final CommitException e) {
					Notification.show(e.getLocalizedMessage());
				}
			}
		}));
		editStationForm.addComponent(new Button("Revert", new ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				fieldGroup.discard();
			}
		}));
		editStationForm.addComponent(new Button("Remove", new ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				final Station bean = fieldGroup.getItemDataSource().getBean();
				stationService.removeStation(bean);
				updateMarkers(googleMap);
			}
		}));
		editStationForm.setEnabled(false);

		// final VerticalLayout editPanel = new VerticalLayout();
		final HorizontalSplitPanel splitPanel = new HorizontalSplitPanel(editStationForm, googleMap);
		splitPanel.setSplitPosition(20, Unit.PERCENTAGE);
		splitPanel.setSizeFull();
		setCompositionRoot(splitPanel);
		setSizeFull();
		if (ne.getLat() != Double.MIN_VALUE) {
			googleMap.fitToBounds(ne, sw);
		}
	}

	private void updateMarkers(final GoogleMap googleMap) {
		editStationForm.setEnabled(false);
		googleMap.clearMarkers();
		for (final Station station : stationService.listAllStations()) {
			drawStation(googleMap, station);
		}
		googleMap.clearPolyLines();
		for (final Connection connection : stationService.listAllConnections()) {
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
			googleMap.addPolyline(polyline);
		}

	}

}
