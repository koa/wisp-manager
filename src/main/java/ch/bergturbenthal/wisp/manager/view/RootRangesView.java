package ch.bergturbenthal.wisp.manager.view;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.navigator.VaadinView;

import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.service.AddressManagementService;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TreeTable;

@VaadinView(name = RootRangesView.VIEW_ID)
public class RootRangesView extends CustomComponent implements View {
	public static final String VIEW_ID = "RootRanges";
	@Autowired
	private AddressManagementService addressManagementBean;

	@Override
	public void enter(final ViewChangeEvent event) {
		addressManagementBean.initAddressRanges();
		final CrudRepositoryContainer<IpRange, Long> connectionContainer = addressManagementBean.createIpContainer();
		setCompositionRoot(new TreeTable("IP Ranges", connectionContainer));
	}
}
