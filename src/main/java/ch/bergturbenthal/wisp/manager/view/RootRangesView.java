package ch.bergturbenthal.wisp.manager.view;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.navigator.VaadinView;

import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.service.AddressManagementService;
import ch.bergturbenthal.wisp.manager.service.IpRangeEntityProvider;

import com.vaadin.addon.jpacontainer.JPAContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;

@VaadinView(name = RootRangesView.VIEW_ID)
public class RootRangesView extends CustomComponent implements View {
	public static final String VIEW_ID = "RootRanges";
	@Autowired
	private AddressManagementService addressManagementBean;
	@Autowired
	private IpRangeEntityProvider ipV4AddressReservationRangeProviderBean;

	@Override
	public void enter(final ViewChangeEvent event) {
		final JPAContainer<IpRange> connectionContainer = new JPAContainer<>(IpRange.class);
		connectionContainer.setEntityProvider(ipV4AddressReservationRangeProviderBean);
		addressManagementBean.initAddressRanges();
		setCompositionRoot(new Label("Empty"));
	}
}
