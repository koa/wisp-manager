package ch.bergturbenthal.wisp.manager.view;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.UIScope;
import org.vaadin.spring.navigator.VaadinView;

import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.service.AddressManagementService;
import ch.bergturbenthal.wisp.manager.util.CrudItem;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;

import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.util.BeanItem;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

@Slf4j
@VaadinView(name = RootRangesView.VIEW_ID)
@UIScope
public class RootRangesView extends CustomComponent implements View {
	public static final String VIEW_ID = "RootRanges";
	@Autowired
	private AddressManagementService addressManagementBean;

	@Override
	public void enter(final ViewChangeEvent event) {
		addressManagementBean.initAddressRanges();
		final CrudRepositoryContainer<IpRange, Long> connectionContainer = addressManagementBean.createIpContainer();
		final TreeTable treeTable = new TreeTable("IP Ranges", connectionContainer);
		treeTable.setVisibleColumns("range", "rangeMask", "type", "comment");

		treeTable.addGeneratedColumn("usage", new ColumnGenerator() {

			@Override
			public String generateCell(final Table source, final Object itemId, final Object columnId) {
				return addressManagementBean.describeRangeUser(connectionContainer.getItem(itemId).getPojo());
			}
		});
		treeTable.setSelectable(true);

		final FormLayout formLayout = new FormLayout();
		final FieldGroup fieldGroup = new FieldGroup(new BeanItem<IpRange>(new IpRange()));
		fieldGroup.setBuffered(false);
		formLayout.addComponent(fieldGroup.buildAndBind("Comment", "comment"));
		formLayout.setVisible(false);
		treeTable.addItemClickListener(new ItemClickListener() {

			@Override
			public void itemClick(final ItemClickEvent event) {
				formLayout.setVisible(true);
				log.info("clicked at " + ((CrudItem<IpRange>) event.getItem()).getPojo());
				fieldGroup.setItemDataSource(event.getItem());
			}
		});

		treeTable.setSizeFull();
		treeTable.setPageLength(0);
		setCompositionRoot(new VerticalLayout(treeTable, formLayout));
	}
}
