package ch.bergturbenthal.wisp.manager.view.component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.util.CrudItem;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer.PojoFilter;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnHeaderMode;

public class StationEditor extends CustomComponent implements ItemEditor<Station> {
	private final CrudRepositoryContainer<NetworkDevice, Long> devicesContainer;
	private final FieldGroup fieldGroup;
	private final FormLayout mainLayout;

	/**
	 * The constructor should first build the main layout, set the composition root and then do any custom initialization.
	 *
	 * The constructor will not be automatically regenerated by the visual editor.
	 */
	public StationEditor(final CrudRepositoryContainer<NetworkDevice, Long> container) {
		devicesContainer = container;
		fieldGroup = new FieldGroup(new BeanItem<Station>(new Station()));
		fieldGroup.setFieldFactory(new CustomFieldFactory(devicesContainer));
		fieldGroup.setBuffered(false);
		mainLayout = new FormLayout();
		mainLayout.setSizeFull();
		setSizeFull();
		mainLayout.addComponent(fieldGroup.buildAndBind("name"));
		mainLayout.addComponent(fieldGroup.buildAndBind("device"));
		mainLayout.addComponent(fieldGroup.buildAndBind("loopbackDescription"));
		final Table connectionTable = new Table();
		connectionTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
		connectionTable.setPageLength(0);
		final BeanItemContainer<Connection> connectionDataSource = new BeanItemContainer<Connection>(Connection.class);
		connectionTable.setContainerDataSource(connectionDataSource);
		connectionTable.setVisibleColumns("title");
		// for (final Object column : connectionTable.getVisibleColumns()) {
		// connectionTable.setColumnExpandRatio(column, 1);
		// }
		connectionTable.setSizeFull();

		final Field<List<Connection>> connectionTableField = new AbstractField<List<Connection>>() {

			@Override
			public Class<? extends List<Connection>> getType() {
				return (Class<? extends List<Connection>>) List.class;
			}

			@Override
			public void setPropertyDataSource(final Property newDataSource) {
				final List<Connection> value = (List<Connection>) newDataSource.getValue();
				connectionDataSource.removeAllItems();
				if (value != null) {
					for (final Connection v : value) {
						connectionDataSource.addBean(v);
					}
				}
				super.setPropertyDataSource(newDataSource);
			}

		};
		fieldGroup.bind(connectionTableField, "connections");
		connectionTable.setCaption("Connections");
		mainLayout.addComponent(connectionTable);
		// mainLayout.addComponent(fieldGroup.buildAndBind("connections"));
		setCompositionRoot(mainLayout);
		mainLayout.setVisible(false);
	}

	@Override
	@SuppressWarnings("unchecked")
	public CrudItem<Station> getCurrentItem() {
		final Item itemDataSource = fieldGroup.getItemDataSource();
		if (itemDataSource instanceof CrudItem) {
			return (CrudItem<Station>) itemDataSource;
		}
		return null;
	}

	@Override
	public void setItem(final CrudItem<Station> item) {
		fieldGroup.setItemDataSource(item);
		devicesContainer.removeAllFilters();
		final Long stationId = item.getPojo().getId();
		final Set<NetworkDeviceModel> stationModels = new HashSet<NetworkDeviceModel>(Arrays.asList(NetworkDeviceModel.stationModels));
		devicesContainer.addFilter(new PojoFilter<NetworkDevice>() {

			@Override
			public boolean accept(final NetworkDevice candidate) {
				if (!stationModels.contains(candidate.getDeviceModel())) {
					return false;
				}
				return candidate.getStation() == null || candidate.getStation().getId().equals(stationId);
			}
		});
		mainLayout.setVisible(true);
	}

}