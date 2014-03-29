package ch.bergturbenthal.wisp.manager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.ejb.EJB;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import ch.bergturbenthal.wisp.manager.service.DemoSetupBean;
import ch.bergturbenthal.wisp.manager.view.ConnectionView;
import ch.bergturbenthal.wisp.manager.view.MapView;
import ch.bergturbenthal.wisp.manager.view.NetworkDeviceView;
import ch.bergturbenthal.wisp.manager.view.RootRangesView;

import com.vaadin.annotations.Widgetset;
import com.vaadin.cdi.CDIUI;
import com.vaadin.cdi.CDIViewProvider;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Layout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@CDIUI
@Widgetset("ch.bergturbenthal.wisp.manager.WispManagerWidgetSet")
@Slf4j
public class WispManagerUI extends UI {
	@EJB
	private DemoSetupBean demoSetupBean;
	@Inject
	private CDIViewProvider viewProvider;

	@Override
	protected void init(final VaadinRequest request) {
		setSizeFull();

		final VerticalLayout navigatorLayout = new VerticalLayout();
		navigatorLayout.setSizeFull();

		final MenuBar menuBar = new MenuBar();
		menuBar.addItem("init", new Command() {

			@Override
			public void menuSelected(final MenuItem selectedItem) {
				try {
					demoSetupBean.initDemoData();
				} catch (final Throwable e) {
					log.error("Cannot init data", e);
				}
			}
		});
		final Map<String, String> menuEntries = new LinkedHashMap<>();
		menuEntries.put("Map", MapView.VIEW_ID);
		menuEntries.put("Connections", ConnectionView.VIEW_ID);
		menuEntries.put("Network Devices", NetworkDeviceView.VIEW_ID);
		menuEntries.put("Network Ranges", RootRangesView.VIEW_ID);

		for (final Entry<String, String> entry : menuEntries.entrySet()) {
			final String viewId = entry.getValue();
			menuBar.addItem(entry.getKey(), new Command() {

				@Override
				public void menuSelected(final MenuItem selectedItem) {
					navigateTo(viewId);
				}
			});

		}

		navigateTo(MapView.VIEW_ID);
		final Layout contentLayout = new VerticalLayout();
		contentLayout.setSizeFull();
		navigatorLayout.addComponent(menuBar);
		navigatorLayout.addComponent(contentLayout);

		navigatorLayout.setExpandRatio(contentLayout, 1);

		final Navigator navigator = new Navigator(WispManagerUI.this, contentLayout);
		navigator.addProvider(viewProvider);

		setContent(navigatorLayout);
	}

	private void navigateTo(final String viewId) {
		Page.getCurrent().setUriFragment("!" + viewId);
	}

}
