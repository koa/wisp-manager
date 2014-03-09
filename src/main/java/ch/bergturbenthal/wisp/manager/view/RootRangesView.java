package ch.bergturbenthal.wisp.manager.view;

import javax.ejb.EJB;

import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.service.AddressManagementBean;
import ch.bergturbenthal.wisp.manager.service.IpRangeProviderBean;

import com.vaadin.addon.jpacontainer.JPAContainer;
import com.vaadin.cdi.CDIView;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;

@CDIView(value = RootRangesView.VIEW_ID)
public class RootRangesView extends CustomComponent implements View {
	public static final String VIEW_ID = "RootRanges";
	@EJB
	private AddressManagementBean addressManagementBean;
	@EJB
	private IpRangeProviderBean ipV4AddressReservationRangeProviderBean;

	@Override
	public void enter(final ViewChangeEvent event) {
		final JPAContainer<IpRange> connectionContainer = new JPAContainer<>(IpRange.class);
		connectionContainer.setEntityProvider(ipV4AddressReservationRangeProviderBean);
		addressManagementBean.initAddressRanges();
		setCompositionRoot(new Label("Empty"));
	}
}
