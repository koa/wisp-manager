package ch.bergturbenthal.wisp.manager.view;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import lombok.Data;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.UIScope;
import org.vaadin.spring.navigator.VaadinView;

import ch.bergturbenthal.wisp.manager.model.Domain;
import ch.bergturbenthal.wisp.manager.model.IpAddress;
import ch.bergturbenthal.wisp.manager.repository.DomainRepository;
import ch.bergturbenthal.wisp.manager.service.DomainService;
import ch.bergturbenthal.wisp.manager.util.BeanReferenceItem;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;
import ch.bergturbenthal.wisp.manager.util.PojoItem;
import ch.bergturbenthal.wisp.manager.view.InputUtils.ResultHandler;
import ch.bergturbenthal.wisp.manager.view.component.IpAddressConverter;
import ch.bergturbenthal.wisp.manager.view.component.ListPropertyTable;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.event.Action;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

@VaadinView(name = DomainView.VIEW_ID)
@UIScope
public class DomainView extends CustomComponent implements View {
	@Data
	public static class InputDomainBean {
		@NotNull
		@javax.validation.constraints.Size(min = 4)
		private String domainName;
	}

	@Data
	public static class InputIpAddressBean {
		@NotNull
		private IpAddress ipAddress;
	}

	public final static String VIEW_ID = "Domains";
	@Autowired
	private DomainRepository domainRepository;

	@Autowired
	private DomainService domainService;

	@Override
	public void enter(final ViewChangeEvent event) {
	}

	@PostConstruct
	public void initDomainView() {
		final VerticalLayout mainLayout = new VerticalLayout();

		final CrudRepositoryContainer<Domain, Long> domainContainer = domainService.createContainerRepository();
		final ComboBox comboBox = new ComboBox("domain", domainContainer);
		comboBox.setItemCaptionPropertyId("domainName");
		mainLayout.addComponent(comboBox);

		final ResultHandler<DomainView.InputDomainBean> handler = new ResultHandler<DomainView.InputDomainBean>() {

			@Override
			public void onCancel() {
				// TODO Auto-generated method stub

			}

			@Override
			public void onOk(final InputDomainBean value) {
				final Domain domain = new Domain();
				domain.setDomainName(value.getDomainName());
				domainRepository.save(domain);
				domainContainer.notifyDataChanged();
			}
		};
		final Window inputDomainNameDialog = InputUtils.createInputDialog(new BeanReferenceItem<InputDomainBean>(new InputDomainBean()), handler, "add Domain", "domainName");

		final Button addDomainButton = new Button("add new Domain", new Button.ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				getUI().addWindow(inputDomainNameDialog);
			}
		});
		mainLayout.addComponent(addDomainButton);

		final FormLayout domainFormlayout = new FormLayout();
		final FieldGroup domainFieldGroup = new FieldGroup(new BeanReferenceItem(Domain.class));
		domainFormlayout.addComponent(domainFieldGroup.buildAndBind("domainName"));
		final ListPropertyTable listPropertyTable = new ListPropertyTable(IpAddress.class, "ip Address");
		domainFieldGroup.bind(listPropertyTable, "dnsServers");
		domainFormlayout.addComponent(listPropertyTable);
		domainFormlayout.addComponent(new Button("Save", new Button.ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				try {
					domainFieldGroup.commit();
				} catch (final CommitException e) {
					throw new RuntimeException(e);
				}
			}
		}));
		listPropertyTable.addGeneratedColumn("value", new Table.ColumnGenerator() {

			@Override
			public Object generateCell(final Table source, final Object itemId, final Object columnId) {
				final Item pojoItem = source.getContainerDataSource().getItem(itemId);
				if (pojoItem == null) {
					return "";
				}
				final TextField textField = new TextField();
				textField.setConverter(new IpAddressConverter());
				textField.setPropertyDataSource(pojoItem.getItemProperty("this"));
				return textField;
			}
		});
		listPropertyTable.setVisibleColumns("value");
		listPropertyTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
		listPropertyTable.setPageLength(0);
		final Action addIpAction = new Action("add IP");
		final Action removeIpAction = new Action("remove IP");
		final ResultHandler<InputIpAddressBean> addIpToDomainHandler = new InputUtils.ResultHandler<DomainView.InputIpAddressBean>() {

			@Override
			public void onCancel() {
			}

			@Override
			public void onOk(final InputIpAddressBean value) {
				final PojoItem<Domain> domainItem = (PojoItem<Domain>) domainFieldGroup.getItemDataSource();
				domainItem.getPojo().getDnsServers().add(value.getIpAddress());
				listPropertyTable.refreshRowCache();
			}
		};
		final Window inputIpAddressDialog = InputUtils.createInputDialog(	new BeanReferenceItem<InputIpAddressBean>(new InputIpAddressBean()),
																																			addIpToDomainHandler,
																																			"add IP",
																																			"ipAddress");
		listPropertyTable.addActionHandler(new Action.Handler() {

			@Override
			public Action[] getActions(final Object target, final Object sender) {
				if (target == null) {
					return new Action[] { addIpAction };
				} else {
					return new Action[] { addIpAction, removeIpAction };
				}
			}

			@Override
			public void handleAction(final Action action, final Object sender, final Object target) {
				if (action == addIpAction) {
					getUI().addWindow(inputIpAddressDialog);
				} else if (action == removeIpAction) {
					final PojoItem<Domain> domainItem = (PojoItem<Domain>) domainFieldGroup.getItemDataSource();
					final PojoItem<IpAddress> selectedIp = (PojoItem<IpAddress>) ((Container) sender).getItem(target);
					domainItem.getPojo().getDnsServers().remove(selectedIp.getPojo());
					listPropertyTable.refreshRowCache();
				}
			}
		});
		domainFormlayout.setEnabled(false);
		mainLayout.addComponent(domainFormlayout);
		comboBox.addValueChangeListener(new Property.ValueChangeListener() {

			@Override
			public void valueChange(final ValueChangeEvent event) {
				final PojoItem<Domain> item = domainContainer.getItem(comboBox.getValue());
				if (item == null) {
					domainFormlayout.setEnabled(false);
				} else {
					domainFormlayout.setEnabled(true);
					domainFieldGroup.setItemDataSource(item);
				}
			}
		});

		setCompositionRoot(mainLayout);
	}
}
