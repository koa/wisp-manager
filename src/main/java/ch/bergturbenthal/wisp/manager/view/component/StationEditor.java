package ch.bergturbenthal.wisp.manager.view.component;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.CustomerConnection;
import ch.bergturbenthal.wisp.manager.model.GatewaySettings;
import ch.bergturbenthal.wisp.manager.model.GatewayType;
import ch.bergturbenthal.wisp.manager.model.IpAddress;
import ch.bergturbenthal.wisp.manager.model.IpNetwork;
import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.NetworkInterface;
import ch.bergturbenthal.wisp.manager.model.RangePair;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.VLan;
import ch.bergturbenthal.wisp.manager.model.address.IpAddressType;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.service.AddressManagementService;
import ch.bergturbenthal.wisp.manager.service.NetworkDeviceManagementService;
import ch.bergturbenthal.wisp.manager.service.StationService;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer.PojoFilter;
import ch.bergturbenthal.wisp.manager.util.PojoItem;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;

@Slf4j
@Component
public class StationEditor extends CustomComponent implements ItemEditor<Station> {

	private static interface Getter<T> {
		T get();
	}

	private class RangePairTextProperty implements Property<String> {
		private final IpAddressType addressType;
		private final Getter<RangePair> rangePairGetter;

		private RangePairTextProperty(final Getter<RangePair> rangePairGetter, final IpAddressType addressType) {
			this.rangePairGetter = rangePairGetter;
			this.addressType = addressType;
		}

		@Override
		public Class<? extends String> getType() {
			return String.class;
		}

		@Override
		public String getValue() {
			return describeCurrentAddress(rangePairGetter.get(), addressType);
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
			addressManagementService.setAddressManually(rangePairGetter.get(), newValue, addressType);
		}
	}

	private static final class TimeSpanConverter implements Converter<String, Long> {
		private static final PeriodFormatter DURATION_FORMAT = new PeriodFormatterBuilder().appendDays()
																																												.appendSuffix("d")
																																												.appendHours()
																																												.appendSuffix("h")
																																												.appendMinutes()
																																												.appendSuffix("m")
																																												.appendSeconds()
																																												.appendSuffix("s")
																																												.toFormatter();

		@Override
		public Long convertToModel(final String value, final Class<? extends Long> targetType, final Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
			if (value == null) {
				return null;
			}
			final Period period = DURATION_FORMAT.parsePeriod(value);
			return Long.valueOf(period.toDurationFrom(new DateTime()).getMillis());
		}

		@Override
		public String convertToPresentation(final Long value, final Class<? extends String> targetType, final Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
			if (value == null) {
				return null;
			}
			return DURATION_FORMAT.print(new Period(value.longValue()));
		}

		@Override
		public Class<Long> getModelType() {
			return Long.class;
		}

		@Override
		public Class<String> getPresentationType() {
			return String.class;
		}
	}

	@Autowired
	private AddressManagementService addressManagementService;
	private NetworkDevice currentNetworkDevice;
	private CrudRepositoryContainer<NetworkDevice, Long> devicesContainer;
	private FieldGroup fieldGroup;
	private FormLayout mainLayout;
	@Autowired
	private NetworkDeviceManagementService networkDeviceManagementService;

	private Button provisionButton;

	@Autowired
	private StationService stationService;

	/**
	 * The constructor should first build the main layout, set the composition root and then do any custom initialization.
	 *
	 * The constructor will not be automatically regenerated by the visual editor.
	 */
	public StationEditor() {
	}

	private Table createCustomerConnectionTable() {
		final ListPropertyContainer<CustomerConnection> listPropertyContainer = new ListPropertyContainer<>(CustomerConnection.class);
		final Table customerConnectionTable = new ListPropertyTable<>(listPropertyContainer);
		customerConnectionTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
		customerConnectionTable.setPageLength(0);
		customerConnectionTable.setSizeFull();
		customerConnectionTable.setCaption("Customer Connection");

		customerConnectionTable.setVisibleColumns("name");
		customerConnectionTable.setEditable(true);

		final Action removeAction = new Action("Remove");
		final Action addAction = new Action("Add");
		final Action editAction = new Action("Edit");
		customerConnectionTable.addActionHandler(new Handler() {

			@Override
			public Action[] getActions(final Object target, final Object sender) {
				return new Action[] { addAction, editAction, removeAction };
			}

			@Override
			public void handleAction(final Action action, final Object sender, final Object target) {
				if (editAction == action) {
					showVLanEditor(listPropertyContainer.getItem(target), getUI());
				} else if (removeAction == action) {
					final PojoItem<CustomerConnection> item = listPropertyContainer.getItem(target);
					if (item == null) {
						return;
					}
					final CustomerConnection customerConnection = item.getPojo();
					if (customerConnection == null) {
						return;
					}
					final Station station = customerConnection.getStation();
					if (station == null) {
						return;
					}
					station.getCustomerConnections().remove(customerConnection);
					customerConnectionTable.refreshRowCache();
				} else if (addAction == action) {
					final Station station = getCurrentStation();
					if (station == null) {
						return;
					}
					final CustomerConnection customerConnection = new CustomerConnection();
					customerConnection.setStation(station);
					station.getCustomerConnections().add(customerConnection);
					customerConnectionTable.refreshRowCache();
				}
			}
		});

		return customerConnectionTable;
	}

	private Property<String> createDhcpEndAddressProperty(final Object itemId, final Container containerDataSource) {
		return new Property<String>() {

			@Override
			public Class<? extends String> getType() {
				return String.class;
			}

			@Override
			public String getValue() {
				final PojoItem<VLan> vlanItem = (PojoItem<VLan>) containerDataSource.getItem(itemId);
				return addressManagementService.getDhcpEndAddress(vlanItem.getPojo());
			}

			@Override
			public boolean isReadOnly() {
				final PojoItem<VLan> vlanItem = (PojoItem<VLan>) containerDataSource.getItem(itemId);
				if (vlanItem == null) {
					return true;
				}
				final VLan vlan = vlanItem.getPojo();
				if (vlan == null) {
					return true;
				}
				final RangePair address = vlan.getAddress();
				if (address == null) {
					return true;
				}
				return address.getV4Address() == null;
			}

			@Override
			public void setReadOnly(final boolean newStatus) {
			}

			@Override
			public void setValue(final String newValue) throws com.vaadin.data.Property.ReadOnlyException {
				final PojoItem<VLan> vlanItem = (PojoItem<VLan>) containerDataSource.getItem(itemId);
				addressManagementService.setDhcpEndAddress(vlanItem.getPojo(), newValue);
			}
		};
	}

	private Property<String> createDhcpStartAddressProperty(final Object itemId, final Container containerDataSource) {
		return new Property<String>() {

			@Override
			public Class<? extends String> getType() {
				return String.class;
			}

			@Override
			public String getValue() {
				final PojoItem<VLan> vlanItem = (PojoItem<VLan>) containerDataSource.getItem(itemId);
				return addressManagementService.getDhcpStartAddress(vlanItem.getPojo());
			}

			@Override
			public boolean isReadOnly() {
				final PojoItem<VLan> vlanItem = (PojoItem<VLan>) containerDataSource.getItem(itemId);
				if (vlanItem == null) {
					return true;
				}
				final VLan vlan = vlanItem.getPojo();
				if (vlan == null) {
					return true;
				}
				final RangePair address = vlan.getAddress();
				if (address == null) {
					return true;
				}
				return address.getV4Address() == null;
			}

			@Override
			public void setReadOnly(final boolean newStatus) {
			}

			@Override
			public void setValue(final String newValue) throws com.vaadin.data.Property.ReadOnlyException {
				final PojoItem<VLan> vlanItem = (PojoItem<VLan>) containerDataSource.getItem(itemId);
				addressManagementService.setDhcpStartAddress(vlanItem.getPojo(), newValue);
			}
		};
	}

	private Table createGatewayTable() {
		final Table gatewayTable = new ListPropertyTable<>(GatewaySettings.class);
		gatewayTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
		gatewayTable.setPageLength(0);
		gatewayTable.setSizeFull();
		gatewayTable.setCaption("Gateways");
		gatewayTable.setEditable(true);
		gatewayTable.addGeneratedColumn("description", new ColumnGenerator() {

			@Override
			public Object generateCell(final Table source, final Object itemId, final Object columnId) {
				final PojoItem<GatewaySettings> gatewayItem = (PojoItem<GatewaySettings>) source.getContainerDataSource().getItem(itemId);
				if (gatewayItem == null) {
					return "";
				}
				final GatewaySettings selectedGateway = gatewayItem.getPojo();
				if (selectedGateway == null) {
					return "";
				}
				final GatewayType gatewayType = selectedGateway.getGatewayType();
				if (gatewayType == null) {
					return "";
				}
				return gatewayType.name();
			}
		});
		gatewayTable.setVisibleColumns("gatewayName", "description");

		final Action removeAction = new Action("Remove");
		final Action addAction = new Action("Add");
		final Action editAction = new Action("Edit");

		gatewayTable.addActionHandler(new Handler() {

			@Override
			public Action[] getActions(final Object target, final Object sender) {
				return new Action[] { addAction, editAction, removeAction };
			}

			@Override
			public void handleAction(final Action action, final Object sender, final Object target) {
				if (addAction == action) {
					final Station station = getCurrentStation();
					if (station == null) {
						return;
					}
					final GatewaySettings gatewaySettings = new GatewaySettings();
					gatewaySettings.setStation(station);
					station.getGatewaySettings().add(gatewaySettings);
					gatewayTable.refreshRowCache();
				} else if (removeAction == action) {
					if (target == null) {
						return;
					}
					final PojoItem<GatewaySettings> gatewayItem = (PojoItem<GatewaySettings>) gatewayTable.getContainerDataSource().getItem(target);
					if (gatewayItem == null) {
						return;
					}
					final GatewaySettings selectedGateway = gatewayItem.getPojo();
					if (selectedGateway == null) {
						return;
					}
					final Station station = selectedGateway.getStation();
					if (station == null) {
						return;
					}
					station.getGatewaySettings().remove(selectedGateway);
					gatewayTable.refreshRowCache();
				} else if (editAction == action) {
					if (target == null) {
						return;
					}
					final PojoItem<GatewaySettings> gatewayItem = (PojoItem<GatewaySettings>) gatewayTable.getContainerDataSource().getItem(target);
					if (gatewayItem == null) {
						return;
					}
					final Window window = new Window("Gateway Settings");
					window.setModal(true);
					final FormLayout formLayout = new FormLayout();
					final FieldGroup formFieldGroup = new FieldGroup(gatewayItem);
					formFieldGroup.setBuffered(false);
					formFieldGroup.setFieldFactory(new CustomFieldFactory(devicesContainer));
					final ComboBox gatewayTypeField = new ComboBox("Type", Arrays.asList(GatewayType.values()));
					gatewayTypeField.setNullSelectionAllowed(false);
					formFieldGroup.bind(gatewayTypeField, "gatewayType");
					formLayout.addComponent(gatewayTypeField);
					formLayout.addComponent(formFieldGroup.buildAndBind("hasIPv4"));
					final Getter<RangePair> rangePairGetter = new Getter<RangePair>() {

						@Override
						public RangePair get() {
							final GatewaySettings gatewaySettings = gatewayItem.getPojo();
							if (gatewaySettings.getManagementAddress() == null) {
								gatewaySettings.setManagementAddress(new RangePair());
							}
							return gatewaySettings.getManagementAddress();
						}
					};
					formLayout.addComponent(new TextField("v4Address", new RangePairTextProperty(rangePairGetter, IpAddressType.V4)));
					formLayout.addComponent(formFieldGroup.buildAndBind("hasIPv6"));
					formLayout.addComponent(new TextField("v6Address", new RangePairTextProperty(rangePairGetter, IpAddressType.V6)));
					formLayout.addComponent(formFieldGroup.buildAndBind("userName"));
					formLayout.addComponent(formFieldGroup.buildAndBind("password"));

					window.setContent(formLayout);
					window.center();
					window.addCloseListener(new CloseListener() {

						@Override
						public void windowClose(final CloseEvent e) {
							gatewayTable.refreshRowCache();
						}
					});

					gatewayTable.getUI().addWindow(window);

				}
			}
		});
		return gatewayTable;
	}

	private ColumnGenerator createVlanAddressEditor(final IpAddressType addressType) {

		return new ColumnGenerator() {

			@Override
			public Object generateCell(final Table source, final Object itemId, final Object columnId) {

				final TextField textField = new TextField(new RangePairTextProperty(new Getter<RangePair>() {

					@Override
					public RangePair get() {
						final VLan bean = readVlanValue(source, itemId);
						if (bean == null) {
							return null;
						}
						if (bean.getAddress() == null) {
							bean.setAddress(new RangePair());
						}
						return bean.getAddress();
					}
				}, addressType));
				textField.setBuffered(false);
				return textField;
			}
		};
	}

	private Table createVlanTable(final PojoItem<CustomerConnection> customerConnectionItem) {
		final Table vlanTable = new ListPropertyTable<>(VLan.class);
		vlanTable.setPageLength(0);
		vlanTable.setSizeFull();
		vlanTable.setCaption("networks");
		vlanTable.addGeneratedColumn("v4Address", createVlanAddressEditor(IpAddressType.V4));
		vlanTable.addGeneratedColumn("v6Address", createVlanAddressEditor(IpAddressType.V6));
		vlanTable.addGeneratedColumn("leaseTime", new ColumnGenerator() {

			@Override
			public Object generateCell(final Table source, final Object itemId, final Object columnId) {
				final TextField textField = new TextField(source.getContainerDataSource().getContainerProperty(itemId, "dhcpSettings.leaseTime"));
				textField.setConverter(new TimeSpanConverter());
				return textField;
			}
		});
		vlanTable.addGeneratedColumn("dhcpStartAddress", new ColumnGenerator() {

			@Override
			public Object generateCell(final Table source, final Object itemId, final Object columnId) {
				return new TextField(createDhcpStartAddressProperty(itemId, source.getContainerDataSource()));
			}

		});

		vlanTable.addGeneratedColumn("dhcpEndAddress", new ColumnGenerator() {

			@Override
			public Object generateCell(final Table source, final Object itemId, final Object columnId) {
				return new TextField(createDhcpEndAddressProperty(itemId, source.getContainerDataSource()));
			}

		});

		vlanTable.setVisibleColumns("vlanId", "v4Address", "v6Address", "leaseTime", "dhcpStartAddress", "dhcpEndAddress");
		vlanTable.setColumnCollapsingAllowed(true);
		vlanTable.setPropertyDataSource(customerConnectionItem.getItemProperty("ownNetworks"));

		vlanTable.setImmediate(true);
		vlanTable.setSelectable(true);
		vlanTable.setSizeFull();
		final Action removeAction = new Action("Remove");
		final Action addAction = new Action("Add");
		vlanTable.addActionHandler(new Handler() {

			@Override
			public Action[] getActions(final Object target, final Object sender) {
				return new Action[] { removeAction, addAction };
			}

			@Override
			public void handleAction(final Action action, final Object sender, final Object target) {
				if (removeAction == action) {
					final PojoItem<VLan> item = (PojoItem<VLan>) vlanTable.getContainerDataSource().getItem(target);
					final VLan vlan = item.getPojo();
					vlan.getCustomerConnection().getOwnNetworks().remove(vlan);
					vlanTable.refreshRowCache();
				} else if (addAction == action) {
					final Window window = new Window("VLan");
					window.setModal(true);
					final FormLayout formLayout = new FormLayout();
					final Property<Integer> vlanId = new ObjectProperty<Integer>(Integer.valueOf(0), Integer.class);
					formLayout.addComponent(new TextField("VLAN id", vlanId));
					formLayout.addComponent(new Button("add", new ClickListener() {

						@Override
						public void buttonClick(final ClickEvent event) {
							final int selectedValue = vlanId.getValue().intValue();
							final CustomerConnection customerConnection = customerConnectionItem.getPojo();
							for (final VLan component : customerConnection.getOwnNetworks()) {
								if (component.getVlanId() == selectedValue) {
									window.close();
									return;
								}
							}
							final VLan vLan = new VLan();
							vLan.setVlanId(selectedValue);
							vLan.setCustomerConnection(customerConnection);
							customerConnection.getOwnNetworks().add(vLan);
							window.close();
							vlanTable.refreshRowCache();
						}
					}));
					window.setContent(formLayout);
					window.center();
					vlanTable.getUI().addWindow(window);

				}
			}
		});

		return vlanTable;
	}

	private String describeCurrentAddress(final RangePair rangePair, final IpAddressType addressType) {
		if (rangePair == null) {
			return null;
		}
		final IpRange ipRange = rangePair.getIpAddress(addressType);
		if (ipRange == null) {
			return null;
		}
		final IpRange parentRange = ipRange.getParentRange();
		if (parentRange == null) {
			return null;
		}
		final int netmask = parentRange.getRange().getNetmask();
		final IpNetwork range = ipRange.getRange();
		if (range == null) {
			return null;
		}
		final IpAddress address = range.getAddress();
		if (address == null) {
			return null;
		}
		final InetAddress inetAddress = address.getInetAddress();
		if (inetAddress == null) {
			return null;
		}
		return inetAddress.getHostAddress() + "/" + netmask;
	}

	private String describeVlanAddress(final VLan bean, final IpAddressType addressType) {
		if (bean == null) {
			return null;
		}
		return describeCurrentAddress(bean.getAddress(), addressType);
	}

	private String getAddressFromVlanItem(final Table source, final Object itemId, final IpAddressType addressType) {
		return describeVlanAddress(readVlanValue(source, itemId), addressType);
	}

	@Override
	@SuppressWarnings("unchecked")
	public PojoItem<Station> getCurrentItem() {
		final Item itemDataSource = fieldGroup.getItemDataSource();
		if (itemDataSource instanceof PojoItem) {
			return (PojoItem<Station>) itemDataSource;
		}
		return null;
	}

	private Station getCurrentStation() {
		final PojoItem<Station> stationItem = getCurrentStationItem();
		if (stationItem == null) {
			return null;
		}
		return stationItem.getPojo();
	}

	private PojoItem<Station> getCurrentStationItem() {
		final PojoItem<Station> stationItem = (PojoItem<Station>) fieldGroup.getItemDataSource();
		return stationItem;
	}

	@PostConstruct
	public void init() {
		devicesContainer = networkDeviceManagementService.createContainerRepository();
		fieldGroup = new FieldGroup(stationService.createContainerRepository().getDummyItem());
		fieldGroup.setFieldFactory(new CustomFieldFactory(devicesContainer));
		fieldGroup.setBuffered(false);
		mainLayout = new FormLayout();
		mainLayout.setSizeFull();
		setSizeFull();
		mainLayout.addComponent(fieldGroup.buildAndBind("name"));
		mainLayout.addComponent(fieldGroup.buildAndBind("device"));

		final ListPropertyTable<NetworkInterface> networkInterfaceTable = new ListPropertyTable<>(NetworkInterface.class);
		// networkInterfaceTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
		networkInterfaceTable.setVisibleColumns("interfaceName", "role", "type");
		networkInterfaceTable.setPageLength(0);
		networkInterfaceTable.setSizeFull();
		networkInterfaceTable.setCaption("Connection configuration");
		fieldGroup.bind(networkInterfaceTable, "device.interfaces");
		mainLayout.addComponent(networkInterfaceTable);

		mainLayout.addComponent(fieldGroup.buildAndBind("tunnelConnection"));
		mainLayout.addComponent(fieldGroup.buildAndBind("loopbackDescription"));

		final Table connectionTable = new ListPropertyTable<>(Connection.class);
		connectionTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
		connectionTable.setPageLength(0);
		connectionTable.setVisibleColumns("title");
		connectionTable.setSizeFull();
		connectionTable.setCaption("Connections");
		fieldGroup.bind(connectionTable, "connections");
		mainLayout.addComponent(connectionTable);

		final Table customerConnectionTable = createCustomerConnectionTable();
		fieldGroup.bind(customerConnectionTable, "customerConnections");
		mainLayout.addComponent(customerConnectionTable);

		final Table gatewayTable = createGatewayTable();
		fieldGroup.bind(gatewayTable, "gatewaySettings");
		mainLayout.addComponent(gatewayTable);

		provisionButton = new Button("provision");
		provisionButton.addClickListener(new ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				final NetworkDevice networkDevice = currentNetworkDevice;
				if (networkDevice != null) {
					networkDeviceManagementService.loadConfig(networkDevice.getV4Address(), networkDevice.getV6Address());
				}
			}
		});
		final Button fillButton = new Button("fill");
		fillButton.addClickListener(new ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				stationService.fillStation(getCurrentStation());

			}
		});
		final VerticalLayout actionButtonsLayout = new VerticalLayout(provisionButton, fillButton);
		actionButtonsLayout.setCaption("Actions");
		mainLayout.addComponent(actionButtonsLayout);
		setCompositionRoot(mainLayout);
		mainLayout.setVisible(false);
	}

	private VLan readVlanValue(final Table source, final Object itemId) {
		final PojoItem<VLan> item = (PojoItem<VLan>) source.getContainerDataSource().getItem(itemId);
		final VLan vLan = item.getPojo();
		return vLan;
	}

	@Override
	public void setItem(final PojoItem<Station> item) {
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
		currentNetworkDevice = item.getPojo().getDevice();
		provisionButton.setEnabled(currentNetworkDevice != null);
		mainLayout.setVisible(true);
	}

	private void showVLanEditor(final PojoItem<CustomerConnection> item, final UI ui) {
		final Window window = new Window("VLan");
		window.setModal(true);
		final Table vlanTable = createVlanTable(item);
		window.setContent(new VerticalLayout(vlanTable));
		window.center();
		ui.addWindow(window);
	}

}
