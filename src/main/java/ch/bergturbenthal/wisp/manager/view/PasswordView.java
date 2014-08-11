package ch.bergturbenthal.wisp.manager.view;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.UIScope;
import org.vaadin.spring.navigator.VaadinView;

import ch.bergturbenthal.wisp.manager.model.Password;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceType;
import ch.bergturbenthal.wisp.manager.repository.PasswordRepository;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Table;

@Slf4j
@VaadinView(name = PasswordView.VIEW_ID)
@UIScope
public class PasswordView extends CustomComponent implements View {

	public static final String VIEW_ID = "password";

	@Autowired
	private PasswordRepository passwordRepository;

	@Override
	public void enter(final ViewChangeEvent event) {

		final CrudRepositoryContainer<Password, NetworkDeviceType> repositoryContainer = new CrudRepositoryContainer<Password, NetworkDeviceType>(passwordRepository,
																																																																							Password.class) {

			@Override
			protected NetworkDeviceType idFromValue(final Password entry) {
				return entry.getDeviceType();
			}

		};
		final Table table = new Table("Passwords", repositoryContainer);
		table.setVisibleColumns("deviceType", "password");
		table.setReadOnly(false);
		table.setSizeFull();
		table.setPageLength(0);
		setCompositionRoot(table);
	}

}
