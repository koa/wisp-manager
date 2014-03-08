package ch.bergturbenthal.wisp.manager;

import javax.inject.Inject;

import ch.bergturbenthal.wisp.manager.view.ConnectionView;
import ch.bergturbenthal.wisp.manager.view.MapView;
import ch.bergturbenthal.wisp.manager.view.NetworkDeviceView;

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
public class WispManagerUI extends UI {
	@Inject
	private CDIViewProvider viewProvider;

	@Override
	protected void init(final VaadinRequest request) {
		setSizeFull();

		final VerticalLayout navigatorLayout = new VerticalLayout();
		navigatorLayout.setSizeFull();

		final MenuBar menuBar = new MenuBar();
		final MenuItem fileMenu = menuBar.addItem("File", null);
		fileMenu.addItem("clear", new Command() {

			@Override
			public void menuSelected(final MenuItem selectedItem) {
				System.out.println("Clear clicked");
			}
		});
		menuBar.addItem("Map", new Command() {

			@Override
			public void menuSelected(final MenuItem selectedItem) {
				navigateTo(MapView.VIEW_ID);
			}
		});
		menuBar.addItem("Connections", new Command() {

			@Override
			public void menuSelected(final MenuItem selectedItem) {
				navigateTo(ConnectionView.VIEW_ID);
			}
		});
		menuBar.addItem("Network Devices", new Command() {

			@Override
			public void menuSelected(final MenuItem selectedItem) {
				navigateTo(NetworkDeviceView.VIEW_ID);
			}
		});
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
