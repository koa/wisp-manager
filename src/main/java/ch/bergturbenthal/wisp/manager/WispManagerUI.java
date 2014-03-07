package ch.bergturbenthal.wisp.manager;

import javax.inject.Inject;

import com.vaadin.cdi.CDIUI;
import com.vaadin.cdi.CDIViewProvider;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@CDIUI
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
		menuBar.addItem("Hello Mein", new Command() {

			@Override
			public void menuSelected(final MenuItem selectedItem) {
				System.out.println("Clicked");
			}
		});

		final CssLayout contentLayout = new CssLayout();
		navigatorLayout.addComponent(menuBar);
		navigatorLayout.addComponent(contentLayout);

		navigatorLayout.setExpandRatio(contentLayout, 1);

		final Navigator navigator = new Navigator(WispManagerUI.this, contentLayout);
		navigator.addProvider(viewProvider);

		setContent(navigatorLayout);
	}

}
