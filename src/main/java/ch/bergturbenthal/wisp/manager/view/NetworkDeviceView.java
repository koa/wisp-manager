package ch.bergturbenthal.wisp.manager.view;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.UIScope;
import org.vaadin.spring.navigator.VaadinView;

import ch.bergturbenthal.wisp.manager.model.Antenna;
import ch.bergturbenthal.wisp.manager.model.MacAddress;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.NetworkInterface;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceType;
import ch.bergturbenthal.wisp.manager.service.ConnectionService;
import ch.bergturbenthal.wisp.manager.service.NetworkDeviceManagementService;
import ch.bergturbenthal.wisp.manager.service.StationService;
import ch.bergturbenthal.wisp.manager.util.CrudItem;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer.PojoFilter;
import ch.bergturbenthal.wisp.manager.view.component.InetAddressConverter;
import ch.bergturbenthal.wisp.manager.view.component.InputIpDialog;
import ch.bergturbenthal.wisp.manager.view.component.InputIpDialog.DialogResultHandler;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@Slf4j
@VaadinView(name = NetworkDeviceView.VIEW_ID)
@UIScope
public class NetworkDeviceView extends CustomComponent implements View {
	public static final String VIEW_ID = "NetworkDevices";
	@Autowired
	private ConnectionService connectionService;
	@Autowired
	private NetworkDeviceManagementService networkDeviceManagementBean;
	@Autowired
	private StationService stationService;

	private ComboBox createAntennaComboBox(final CrudItem<NetworkDevice> deviceItem, final CrudRepositoryContainer<Antenna, Long> antennaContainer) {
		final NetworkDevice networkDevice = deviceItem.getPojo();
		antennaContainer.removeAllFilters();
		antennaContainer.addFilter(new PojoFilter<Antenna>() {
			@Override
			public boolean accept(final Antenna candidate) {
				return candidate.getDevice() == null || candidate.getDevice().getId() == networkDevice.getId();
			}
		});
		final ComboBox antennaComboBox = new ComboBox("Station", antennaContainer);
		antennaComboBox.setInvalidAllowed(false);
		if (networkDevice.getAntenna() != null) {
			antennaComboBox.setValue(networkDevice.getAntenna().getId());
		} else {
			antennaComboBox.setValue(null);
		}
		antennaComboBox.setItemCaptionPropertyId("title");
		antennaComboBox.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(final ValueChangeEvent event) {
				final Object selectedIndex = event.getProperty().getValue();
				final NetworkDevice networkDevice = deviceItem.getPojo();
				final Antenna oldAntenna = networkDevice.getAntenna();
				if (oldAntenna != null) {
					oldAntenna.setDevice(null);
				}
				if (selectedIndex != null) {
					final Antenna selectedAntenna = antennaContainer.getItem(selectedIndex).getPojo();
					selectedAntenna.setDevice(networkDevice);
				}
			}
		});
		return antennaComboBox;
	}

	private ComboBox createStationComboBox(final CrudItem<NetworkDevice> deviceItem, final CrudRepositoryContainer<Station, Long> stationContainer) {
		final NetworkDevice networkDevice = deviceItem.getPojo();
		stationContainer.removeAllFilters();
		stationContainer.addFilter(new PojoFilter<Station>() {
			@Override
			public boolean accept(final Station candidate) {
				return candidate.getDevice() == null || candidate.getDevice().getId() == networkDevice.getId();
			}
		});
		final ComboBox stationComboBox = new ComboBox("Station", stationContainer);
		stationComboBox.setInvalidAllowed(false);
		if (networkDevice.getStation() != null) {
			stationComboBox.setValue(networkDevice.getStation().getId());
		} else {
			stationComboBox.setValue(null);
		}
		stationComboBox.setItemCaptionPropertyId("name");
		stationComboBox.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(final ValueChangeEvent event) {
				final Object selectedIndex = event.getProperty().getValue();
				final NetworkDevice networkDevice = deviceItem.getPojo();
				final Station oldStation = networkDevice.getStation();
				if (oldStation != null) {
					oldStation.setDevice(null);
				}
				if (selectedIndex != null) {
					final Station selectedStation = stationContainer.getItem(selectedIndex).getPojo();
					selectedStation.setDevice(networkDevice);
				}
			}
		});
		return stationComboBox;
	}

	@Override
	public void enter(final ViewChangeEvent event) {

		final CrudRepositoryContainer<NetworkDevice, Long> devicesContainer = networkDeviceManagementBean.createContainerRepository();
		final CrudRepositoryContainer<Station, Long> stationContainer = stationService.createContainerRepository();
		final CrudRepositoryContainer<Antenna, Long> antennaContainer = connectionService.createAntennaContainer();

		final HorizontalLayout horizontalLayout = new HorizontalLayout();
		final ListSelect deviceSelect = new ListSelect("Select a Network Device", devicesContainer);
		deviceSelect.setItemCaptionMode(ItemCaptionMode.PROPERTY);
		deviceSelect.setItemCaptionPropertyId("title");
		final VerticalLayout selectDeviceLayout = new VerticalLayout();
		selectDeviceLayout.addComponent(deviceSelect);

		for (final NetworkDeviceModel model : NetworkDeviceModel.values()) {
			selectDeviceLayout.addComponent(new Button("add " + model, new ClickListener() {

				@Override
				public void buttonClick(final ClickEvent event) {
					networkDeviceManagementBean.createDevice(model);
					devicesContainer.notifyDataChanged();
				}
			}));
		}
		selectDeviceLayout.addComponent(new Button("remove Device", new ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				final Object selectedValue = deviceSelect.getValue();
				if (selectedValue != null) {
					devicesContainer.removeItem(selectedValue);
				}
			}
		}));
		selectDeviceLayout.addComponent(new Button("identify by address", new ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				try {
					InputIpDialog.show(getUI(), "Enter IP", InetAddress.getByName("192.168.88.1"), new DialogResultHandler() {

						@Override
						public void takeIp(final InetAddress address) {
							final NetworkDevice detectNetworkDevice = networkDeviceManagementBean.detectNetworkDevice(address);
							devicesContainer.notifyDataChanged();
							deviceSelect.select(detectNetworkDevice.getId());
						}
					});

				} catch (final Throwable e) {
					e.printStackTrace();
				}
			}
		}));
		selectDeviceLayout.addComponent(new Button("scan for devices", new ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				final Collection<NetworkDevice> foundDevices = networkDeviceManagementBean.scanForDevices();
				devicesContainer.notifyDataChanged();
				final Iterator<NetworkDevice> iterator = foundDevices.iterator();
				if (iterator.hasNext()) {
					deviceSelect.select(iterator.next().getId());
				}
			}
		}));
		selectDeviceLayout.addComponent(new Button("provision 192.168.88.1", new ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				try {
					networkDeviceManagementBean.loadConfig(InetAddress.getByName("192.168.88.1"));
				} catch (final Throwable e) {
					e.printStackTrace();
				}
			}
		}));

		final FormLayout editDeviceForm = new FormLayout();
		editDeviceForm.setEnabled(false);
		deviceSelect.addValueChangeListener(new ValueChangeListener() {

			private TextField createInetAddressField(final CrudItem<NetworkDevice> deviceItem, final String fieldName) {
				final TextField field = withConverter(new TextField(fieldName), InetAddressConverter.getInstance());
				field.setPropertyDataSource(deviceItem.getItemProperty(fieldName));
				field.setNullRepresentation("");
				return field;
			}

			@Override
			public void valueChange(final ValueChangeEvent event) {

				final Long deviceId = (Long) event.getProperty().getValue();
				final CrudItem<NetworkDevice> deviceItem = devicesContainer.getItem(deviceId);
				final NetworkDevice networkDevice = deviceItem.getPojo();
				editDeviceForm.removeAllComponents();
				editDeviceForm.addComponent(new Label(deviceItem.getItemProperty("title")));
				@SuppressWarnings("unchecked")
				final Property<List<NetworkInterface>> itemProperty = deviceItem.getItemProperty("interfaces");
				final Property<String> macAddressDataSource = new Property<String>() {

					@Override
					public Class<? extends String> getType() {
						return String.class;
					}

					@Override
					public String getValue() {
						return itemProperty.getValue().get(0).getMacAddress().getAddress();
					}

					@Override
					public boolean isReadOnly() {
						return false;
					}

					@Override
					public void setReadOnly(final boolean newStatus) {

					}

					@Override
					public void setValue(final String newValue) throws com.vaadin.data.Property.ReadOnlyException {
						final NetworkDevice device = deviceItem.getPojo();
						final List<NetworkInterface> interfaces = device.getInterfaces();
						final Iterator<MacAddress> macAddressIterator = device.getDeviceModel()
																																	.getAddressIncrementorFactory()
																																	.getAllMacAddresses(new MacAddress(newValue))
																																	.iterator();
						for (final NetworkInterface networkInterface : interfaces) {
							networkInterface.setMacAddress(macAddressIterator.next());
						}
						itemProperty.setValue(interfaces);
					}
				};
				editDeviceForm.addComponent(new TextField("Serial Number", deviceItem.getItemProperty("serialNumber")));
				editDeviceForm.addComponent(new TextField("Base-Address", macAddressDataSource));
				final TextField passwordTextField = new TextField("Admin Password", deviceItem.getItemProperty("currentPassword"));
				passwordTextField.setReadOnly(true);
				editDeviceForm.addComponent(passwordTextField);
				if (networkDevice.getDeviceModel().getDeviceType() == NetworkDeviceType.STATION) {
					editDeviceForm.addComponent(createStationComboBox(deviceItem, stationContainer));
				} else if (networkDevice.getDeviceModel().getDeviceType() == NetworkDeviceType.ANTENNA) {
					editDeviceForm.addComponent(createAntennaComboBox(deviceItem, antennaContainer));
				}
				editDeviceForm.addComponent(createInetAddressField(deviceItem, "v4Address"));
				editDeviceForm.addComponent(createInetAddressField(deviceItem, "v6Address"));

				// editDeviceForm.addComponent(withConverter(new Label(deviceItem.getItemProperty("v6Address")), new
				// InetAddressConverter<>(Inet6Address.class)));

				final BeanItemContainer<NetworkInterface> dataSource = new BeanItemContainer<>(NetworkInterface.class, deviceItem.getPojo().getInterfaces());
				dataSource.addNestedContainerProperty("macAddress.address");
				final Table table = new Table("interfaces", dataSource);
				table.setVisibleColumns("type", "macAddress.address");
				table.setPageLength(0);

				editDeviceForm.addComponent(table);

				editDeviceForm.addComponent(new Button("Save", new ClickListener() {

					@Override
					public void buttonClick(final ClickEvent event) {
						table.commit();
						editDeviceForm.setEnabled(false);
					}
				}));
				final Button provisionDeviceButton = new Button("Provision", new ClickListener() {

					@Override
					public void buttonClick(final ClickEvent event) {
						table.commit();
						log.info("new Configuration: " + networkDeviceManagementBean.generateConfig(deviceItem.getPojo()));
						final NetworkDevice networkDevice = deviceItem.getPojo();
						if (isReachable(networkDevice.getV4Address())) {
							networkDeviceManagementBean.loadConfig(networkDevice.getV4Address());
						} else if (isReachable(networkDevice.getV6Address())) {
							networkDeviceManagementBean.loadConfig(networkDevice.getV6Address());
						}

						editDeviceForm.setEnabled(false);
					}
				});
				provisionDeviceButton.setEnabled(!networkDevice.isProvisioned());
				editDeviceForm.addComponent(provisionDeviceButton);

				editDeviceForm.setEnabled(true);
			}

		});
		deviceSelect.setImmediate(true);
		deviceSelect.setNullSelectionAllowed(false);
		horizontalLayout.addComponent(selectDeviceLayout);
		horizontalLayout.addComponent(editDeviceForm);
		setCompositionRoot(horizontalLayout);
		horizontalLayout.setSizeFull();
		setSizeFull();

	}

	private boolean isReachable(final InetAddress address) {
		if (address == null) {
			return false;
		}
		try {
			return address.isReachable(150);
		} catch (final IOException e) {
			return false;
		}
	}

	private <C extends Component> C withConverter(final C component, final Converter<String, ?> converter) {
		if (component instanceof Label) {
			((Label) component).setConverter(converter);
		}
		if (component instanceof AbstractTextField) {
			((AbstractTextField) component).setConverter(converter);
		}
		return component;
	}
}
