package ch.bergturbenthal.wisp.manager.view;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.UIScope;
import org.vaadin.spring.navigator.VaadinView;

import ch.bergturbenthal.wisp.manager.model.IpNetwork;
import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.model.address.AddressRangeType;
import ch.bergturbenthal.wisp.manager.service.AddressManagementService;
import ch.bergturbenthal.wisp.manager.util.BeanReferenceItem;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;
import ch.bergturbenthal.wisp.manager.util.PojoItem;
import ch.bergturbenthal.wisp.manager.view.component.CustomFieldGroupFactory;

import com.vaadin.data.Validator;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.util.BeanItem;
import com.vaadin.event.Action;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

@Slf4j
@VaadinView(name = RootRangesView.VIEW_ID)
@UIScope
public class RootRangesView extends CustomComponent implements View {
	@Data
	public static class AppendChildRangeData {
		private PojoItem<IpRange> parentRangeItem;
		@NotNull
		private Integer subRangeSize;
		@NotNull
		private AddressRangeType type;

		public IpRange getParentRange() {
			return parentRangeItem.getPojo();
		}
	}

	@Data
	public static class AppendRootRangeData {
		@NotNull
		private IpNetwork address;
		@NotNull
		@Min(8)
		@Max(128)
		private Integer reservationMask;
	}

	public static final String VIEW_ID = "RootRanges";
	@Autowired
	private AddressManagementService addressManagementBean;
	private FieldGroup addRootRangeFieldGroup;
	private Window addRootRangeWindow;
	private FieldGroup addSubRangeFieldGroup;
	private Window addSubRangeWindow;
	private CrudRepositoryContainer<IpRange, Long> connectionContainer;

	@Override
	public void enter(final ViewChangeEvent event) {
	}

	@PostConstruct
	public void initAddRootRangeWindow() {
		addRootRangeWindow = new Window("Append root range");
		addRootRangeWindow.setModal(true);
		final FormLayout layout = new FormLayout();
		addRootRangeFieldGroup = new FieldGroup(new BeanReferenceItem<AppendRootRangeData>(AppendRootRangeData.class));
		addRootRangeFieldGroup.setFieldFactory(new CustomFieldGroupFactory());
		layout.addComponent(addRootRangeFieldGroup.buildAndBind("address"));
		layout.addComponent(addRootRangeFieldGroup.buildAndBind("reservationMask"));

		layout.addComponent(new Button("Ok", new Button.ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				try {
					addRootRangeFieldGroup.commit();
					final AppendRootRangeData inputData = ((PojoItem<AppendRootRangeData>) addRootRangeFieldGroup.getItemDataSource()).getPojo();
					addressManagementBean.addRootRange(inputData.getAddress(), inputData.getReservationMask(), "");
					addRootRangeWindow.close();
					refreshTable();
				} catch (final CommitException e) {
					log.error("Commit error", e);
				}
			}
		}));
		addRootRangeWindow.setContent(layout);
		addRootRangeWindow.center();

	}

	@PostConstruct
	public void initAddSubRangeWindow() {
		addSubRangeWindow = new Window("Append Range");
		addSubRangeWindow.setModal(true);
		final FormLayout layout = new FormLayout();
		addSubRangeFieldGroup = new FieldGroup(new BeanReferenceItem<AppendChildRangeData>(AppendChildRangeData.class));
		addSubRangeFieldGroup.setFieldFactory(new CustomFieldGroupFactory());

		final Field<?> parentRangeField = addSubRangeFieldGroup.buildAndBind("Parent range", "parentRange.range.description");
		parentRangeField.setWidth("100%");
		layout.addComponent(parentRangeField);

		final Field<?> subRangeSizeField = addSubRangeFieldGroup.buildAndBind("sub size", "subRangeSize");
		subRangeSizeField.addValidator(new Validator() {

			@Override
			public void validate(final Object value) throws InvalidValueException {
				final Integer subRange = (Integer) value;
				if (subRange == null) {
					return;
				}
				@SuppressWarnings("unchecked")
				final AppendChildRangeData appendChildRangeData = ((PojoItem<AppendChildRangeData>) addSubRangeFieldGroup.getItemDataSource()).getPojo();
				final IpRange parentRange = appendChildRangeData.getParentRange();
				final int rangeMask = subRange.intValue();
				final int minBitCount = parentRange.getRangeMask();
				final int maxBitCount = parentRange.getRange().getAddress().getAddressType().getBitCount();
				if (rangeMask < minBitCount) {
					throw new InvalidValueException("must be greater or equal " + minBitCount);
				}
				if (rangeMask > maxBitCount) {
					throw new InvalidValueException("must be smaller or equal " + maxBitCount);
				}
			}
		});
		layout.addComponent(subRangeSizeField);
		layout.addComponent(addSubRangeFieldGroup.buildAndBind("reservation type", "type", ComboBox.class));
		layout.addComponent(new Button("Ok", new Button.ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				try {
					addSubRangeFieldGroup.commit();
					final AppendChildRangeData inputData = ((PojoItem<AppendChildRangeData>) addSubRangeFieldGroup.getItemDataSource()).getPojo();
					final IpRange parentRange = inputData.getParentRange();
					final int subRangeSize = inputData.getSubRangeSize().intValue();
					final AddressRangeType type = inputData.getType();
					addressManagementBean.reserveRange(parentRange, type, subRangeSize, "");
					addSubRangeWindow.close();
					refreshTable();
				} catch (final CommitException e) {
					log.error("Commit error", e);
				}
			}
		}));
		addSubRangeWindow.setContent(layout);
		addSubRangeWindow.center();

	}

	@PostConstruct
	public void initView() {
		addressManagementBean.initAddressRanges();
		connectionContainer = addressManagementBean.createIpContainer();
		final TreeTable treeTable = new TreeTable("IP Ranges", connectionContainer);
		treeTable.setVisibleColumns("range", "rangeMask", "type", "comment");

		treeTable.addGeneratedColumn("usage", new ColumnGenerator() {
			@Override
			public String generateCell(final Table source, final Object itemId, final Object columnId) {
				return addressManagementBean.describeRangeUser(connectionContainer.getItem(itemId).getPojo());
			}
		});
		treeTable.addGeneratedColumn("orphan", new ColumnGenerator() {

			@Override
			public Object generateCell(final Table source, final Object itemId, final Object columnId) {
				return connectionContainer.getItem(itemId).getPojo().isOrphan() ? "x" : "";
			}
		});
		treeTable.setSelectable(true);

		final FormLayout editRangeLayout = new FormLayout();
		final FieldGroup fieldGroup = new FieldGroup(new BeanItem<IpRange>(new IpRange()));
		fieldGroup.setBuffered(false);
		editRangeLayout.addComponent(fieldGroup.buildAndBind("Comment", "comment"));
		editRangeLayout.setVisible(false);
		treeTable.addItemClickListener(new ItemClickListener() {

			@Override
			public void itemClick(final ItemClickEvent event) {
				editRangeLayout.setVisible(true);
				fieldGroup.setItemDataSource(event.getItem());
			}
		});

		final Action removeAction = new Action("remove");
		final Action addRootAction = new Action("append root");
		final Action addChildAction = new Action("add child");

		treeTable.addActionHandler(new Action.Handler() {

			@Override
			public Action[] getActions(final Object target, final Object sender) {
				final List<Action> actions = new ArrayList<Action>();
				actions.add(addRootAction);
				if (target != null) {
					final IpRange ipRange = connectionContainer.getItem(target).getPojo();
					if (ipRange.getAvailableReservations() > 0) {
						actions.add(addChildAction);
					}
					if (ipRange.isOrphan()) {
						actions.add(removeAction);
					}
				}
				return actions.toArray(new Action[actions.size()]);
			}

			@Override
			public void handleAction(final Action action, final Object sender, final Object target) {
				if (removeAction == action) {
					final IpRange ipRange = connectionContainer.getItem(target).getPojo();
					addressManagementBean.removeRange(ipRange);
					refreshTable();
				} else if (action == addRootAction) {
					addRootRangeFieldGroup.setItemDataSource(new BeanReferenceItem<AppendRootRangeData>(new AppendRootRangeData()));
					getUI().addWindow(addRootRangeWindow);
				} else if (action == addChildAction) {
					final PojoItem<IpRange> ipRangeItem = connectionContainer.getItem(target);
					final IpRange ipRange = ipRangeItem.getPojo();
					final AppendChildRangeData childRangeData = new AppendChildRangeData();
					final int defaultMask;
					switch (ipRange.getRange().getAddress().getAddressType()) {
					case V4:
						defaultMask = 24;
						break;
					case V6:
						defaultMask = 64;
						break;
					default:
						throw new IllegalArgumentException("Unkown address type of " + ipRange + " (" + ipRange.getRange().getAddress().getAddressType() + ")");
					}
					if (defaultMask < ipRange.getRangeMask()) {
						childRangeData.setSubRangeSize(Integer.valueOf(ipRange.getRangeMask()));
					} else {
						childRangeData.setSubRangeSize(Integer.valueOf(defaultMask));
					}
					childRangeData.setType(AddressRangeType.ADMINISTRATIVE);
					childRangeData.setParentRangeItem(ipRangeItem);
					addSubRangeFieldGroup.setItemDataSource(new BeanReferenceItem<AppendChildRangeData>(childRangeData));
					getUI().addWindow(addSubRangeWindow);
				}
			}
		});

		treeTable.setSizeFull();
		treeTable.setPageLength(0);
		final Button cleanupButton = new Button("cleanup Ranges", new Button.ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				addressManagementBean.cleanupOrphanRanges();
				refreshTable();
			}
		});
		setCompositionRoot(new VerticalLayout(treeTable, editRangeLayout, new HorizontalLayout(cleanupButton)));
	}

	private void refreshTable() {
		connectionContainer.notifyDataChanged();
	}
}
