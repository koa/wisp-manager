package ch.bergturbenthal.wisp.manager.view;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.navigator.VaadinView;

import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.service.AddressManagementService;
import ch.bergturbenthal.wisp.manager.service.CurrentEntityManagerHolder;

import com.vaadin.addon.jpacontainer.JPAContainer;
import com.vaadin.addon.jpacontainer.JPAContainerFactory;
import com.vaadin.addon.jpacontainer.provider.CachingLocalEntityProvider;
import com.vaadin.addon.jpacontainer.util.HibernateLazyLoadingDelegate;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Table;

@VaadinView(name = RootRangesView.VIEW_ID)
public class RootRangesView extends CustomComponent implements View {
	public static final String VIEW_ID = "RootRanges";
	@Autowired
	private AddressManagementService addressManagementBean;
	@Autowired
	private CurrentEntityManagerHolder entityManagerHolder;

	@Override
	public void enter(final ViewChangeEvent event) {
		addressManagementBean.initAddressRanges();
		final JPAContainer<IpRange> connectionContainer = JPAContainerFactory.make(IpRange.class, entityManagerHolder.getCurrentEntityManager());
		final HibernateLazyLoadingDelegate hibernateLazyLoadingDelegate = new HibernateLazyLoadingDelegate();
		hibernateLazyLoadingDelegate.setEntityProvider(new CachingLocalEntityProvider<IpRange>(IpRange.class, entityManagerHolder.getCurrentEntityManager()));
		connectionContainer.getEntityProvider().setLazyLoadingDelegate(hibernateLazyLoadingDelegate);
		setCompositionRoot(new Table("IP Ranges", connectionContainer));
	}
}
