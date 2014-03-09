package ch.bergturbenthal.wisp.manager.view;

import java.util.Arrays;
import java.util.Locale;

import javax.ejb.EJB;

import org.vaadin.dialogs.ConfirmDialog;

import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.Position;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.service.ConnectionService;
import ch.bergturbenthal.wisp.manager.service.NetworkDeviceProviderBean;
import ch.bergturbenthal.wisp.manager.service.StationProviderBean;
import ch.bergturbenthal.wisp.manager.service.StationService;
import ch.bergturbenthal.wisp.manager.view.map.GoogleMap;

import com.vaadin.addon.jpacontainer.EntityItem;
import com.vaadin.addon.jpacontainer.JPAContainer;
import com.vaadin.addon.jpacontainer.filter.Filters;
import com.vaadin.cdi.CDIView;
import com.vaadin.data.Container.Filter;
import com.vaadin.data.Property;
import com.vaadin.data.fieldgroup.DefaultFieldGroupFieldFactory;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.converter.Converter;
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
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;

@CDIView(value = MapView.VIEW_ID)
public class MapView extends CustomComponent implements View {

	public static final String VIEW_ID = "Map";
	@EJB
	private ConnectionService connectionService;
	private JPAContainer<NetworkDevice> devicesContainer;
	private FormLayout editStationForm;
	@EJB
	private NetworkDeviceProviderBean networkDeviceProviderBean;
	@EJB
	private StationProviderBean stationProviderBean;
	@EJB
	private StationService stationService;

	private void activateStation(final FieldGroup fieldGroup, final EntityItem<Station> stationItem) {
		fieldGroup.setItemDataSource(stationItem);
		devicesContainer.removeAllContainerFilters();
		devicesContainer.addContainerFilter(Filters.or(Filters.isNull("station"), Filters.eq("station", stationItem.getEntity())));
		final Filter[] modelFilters = new Filter[NetworkDeviceModel.stationModels.length];
		for (int i = 0; i < modelFilters.length; i++) {
			modelFilters[i] = Filters.eq("deviceModel", NetworkDeviceModel.stationModels[i]);
		}
		devicesContainer.addContainerFilter(Filters.or(modelFilters));
		editStationForm.setEnabled(true);
	}

	private void doIfDiscardOk(final FieldGroup fieldGroup, final Runnable runnable) {
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

		final JPAContainer<Station> stationContainer = new JPAContainer<>(Station.class);
		stationContainer.setEntityProvider(stationProviderBean);
		devicesContainer = new JPAContainer<>(NetworkDevice.class);
		devicesContainer.setEntityProvider(networkDeviceProviderBean);

		editStationForm = new FormLayout();

		final Station emptyStation = new Station();
		emptyStation.setDevice(new NetworkDevice());
		final FieldGroup fieldGroup = new FieldGroup(new BeanItem<Station>(emptyStation));
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
						activateStation(fieldGroup, stationContainer.getItem(newStation.getId()));
					}
				});
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
				doIfDiscardOk(fieldGroup, new Runnable() {
					@Override
					public void run() {
						final EntityItem<Station> stationItem = stationContainer.getItem(Long.valueOf(clickedMarker.getId()));
						activateStation(fieldGroup, stationItem);
					}
				});
			}
		});
		updateMarkers(googleMap);
		googleMap.setSizeFull();
		fieldGroup.setFieldFactory(new DefaultFieldGroupFieldFactory() {

			@Override
			public <T extends Field> T createField(final Class<?> dataType, final Class<T> fieldType) {
				if (dataType.isAssignableFrom(NetworkDevice.class)) {
					final ComboBox comboBox = new ComboBox("Device", devicesContainer);
					comboBox.setItemCaptionPropertyId("title");
					comboBox.setConverter(new Converter<Object, NetworkDevice>() {

						@Override
						public NetworkDevice convertToModel(final Object value, final Class<? extends NetworkDevice> targetType, final Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
							if (value == null) {
								return new NetworkDevice();
							}
							return devicesContainer.getItem(value).getEntity();
						}

						@Override
						public Object convertToPresentation(final NetworkDevice value, final Class<? extends Object> targetType, final Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
							if (value == null) {
								return null;
							}
							return value.getId();
						}

						@Override
						public Class<NetworkDevice> getModelType() {
							return NetworkDevice.class;
						}

						@Override
						public Class<Object> getPresentationType() {
							// TODO Auto-generated method stub
							return Object.class;
						}
					});
					return (T) comboBox;
				}
				return super.createField(dataType, fieldType);
			}
		});
		editStationForm.addComponent(fieldGroup.buildAndBind("Name", "name"));
		editStationForm.addComponent(fieldGroup.buildAndBind("Device", "device"));

		editStationForm.addComponent(new Button("Save", new ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				try {
					fieldGroup.commit();
					// final Station bean = fieldGroup.getItemDataSource().getBean();
					// stationService.updateStation(bean);
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
				@SuppressWarnings("unchecked")
				final Property<Long> idProperty = fieldGroup.getItemDataSource().getItemProperty("id");
				stationContainer.removeItem(idProperty.getValue());
				updateMarkers(googleMap);
			}
		}));
		editStationForm.setEnabled(false);

		final VerticalLayout editPanel = new VerticalLayout(editStationForm);
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
			googleMap.addPolyline(polyline);
		}
	}

	private void updateMarkers(final GoogleMap googleMap) {
		editStationForm.setEnabled(false);
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
