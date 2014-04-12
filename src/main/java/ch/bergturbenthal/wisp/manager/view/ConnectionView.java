package ch.bergturbenthal.wisp.manager.view;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.navigator.VaadinView;

import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.service.ConnectionService;
import ch.bergturbenthal.wisp.manager.service.GeoService;
import ch.bergturbenthal.wisp.manager.service.StationService;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;

import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.data.util.AbstractBeanContainer.BeanIdResolver;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.VerticalLayout;

@VaadinView(name = ConnectionView.VIEW_ID)
public class ConnectionView extends CustomComponent implements View {
	public static final String VIEW_ID = "Connections";
	@Autowired
	private ConnectionService connectionService;
	@Autowired
	private GeoService geoService;
	@Autowired
	private StationService stationService;

	@Override
	public void enter(final ViewChangeEvent event) {
		final CrudRepositoryContainer<Connection, Long> connectionContainer = connectionService.makeContainer();
		final BeanContainer<String, Station> beanContainer = new BeanContainer<>(Station.class);
		beanContainer.setBeanIdResolver(new BeanIdResolver<String, Station>() {
			@Override
			public String getIdForBean(final Station bean) {
				return bean.getName();
			}
		});
		beanContainer.addAll(stationService.listAllStations());
		final Map<String, Station> reverseMapping = new HashMap<String, Station>();
		for (final String name : beanContainer.getItemIds()) {
			reverseMapping.put(name, beanContainer.getItem(name).getBean());
		}
		final Table table = new Table("Connections", connectionContainer);
		table.setEditable(true);
		table.setVisibleColumns("startStation", "endStation");
		table.setTableFieldFactory(new TableFieldFactory() {

			@Override
			public Field<?> createField(final Container container, final Object itemId, final Object propertyId, final Component uiContext) {
				final Property containerProperty = container.getContainerProperty(itemId, propertyId);
				final Class<?> type = containerProperty.getType();
				if (type.isAssignableFrom(Station.class)) {
					final ComboBox comboBox = new ComboBox("select Station", beanContainer);
					@SuppressWarnings("unchecked")
					final Converter<Object, ?> converter = (Converter<Object, ?>) (Converter<?, ?>) new Converter<String, Station>() {

						@Override
						public Station convertToModel(final String value, final Class<? extends Station> targetType, final Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
							return reverseMapping.get(value);
						}

						@Override
						public String convertToPresentation(final Station value, final Class<? extends String> targetType, final Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
							if (value == null) {
								return "";
							}
							return value.getName();
						}

						@Override
						public Class<Station> getModelType() {
							return Station.class;
						}

						@Override
						public Class<String> getPresentationType() {
							return String.class;
						}

					};
					comboBox.setConverter(converter);
					return comboBox;
				}
				return DefaultFieldFactory.get().createField(connectionContainer, itemId, propertyId, uiContext);
			}
		});

		table.addGeneratedColumn("distance", new ColumnGenerator() {

			@Override
			public Component generateCell(final Table source, final Object itemId, final Object columnId) {
				final Connection connection = connectionContainer.getItem(itemId).getPojo();
				final Station startStation = connection.getStartStation();
				final Station endStation = connection.getEndStation();
				if (startStation == null || endStation == null) {
					return new Label("");
				}
				if (startStation.getPosition() == null || endStation.getPosition() == null) {
					return new Label("");
				}
				final double distance = geoService.distance(startStation.getPosition(), endStation.getPosition());
				return new Label(NumberFormat.getIntegerInstance().format(distance) + " m");
			}
		});
		table.addGeneratedColumn("remove", new ColumnGenerator() {

			@Override
			public Component generateCell(final Table source, final Object itemId, final Object columnId) {
				return new Button("remove", new ClickListener() {

					@Override
					public void buttonClick(final ClickEvent event) {
						connectionContainer.removeItem(itemId);
					}
				});
			}
		});

		table.setSizeFull();

		final Button addButton = new Button("Add", new ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				connectionService.connectStations(null, null);
				table.refreshRowCache();
				// connectionContainer.addEntity(new Connection());
			}
		});

		final Button saveButton = new Button("save", new ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				table.commit();
				// connectionContainer.commit();
			}
		});
		// connectionContainer.setAutoCommit(true);
		final VerticalLayout verticalLayout = new VerticalLayout(table, new HorizontalLayout(addButton, saveButton));
		verticalLayout.setSizeFull();
		verticalLayout.setExpandRatio(table, 1);
		setCompositionRoot(verticalLayout);
		setSizeFull();
	}
}
