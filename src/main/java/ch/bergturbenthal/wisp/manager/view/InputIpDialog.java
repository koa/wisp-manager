package ch.bergturbenthal.wisp.manager.view;

import java.net.InetAddress;

import com.vaadin.data.Property;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.fieldgroup.FieldGroupFieldFactory;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.util.PropertysetItem;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

public class InputIpDialog extends Window {
	public static interface DialogResultHandler {
		void takeIp(final InetAddress address);
	}

	public static void show(final UI ui, final String question, final InetAddress defaultAddress, final DialogResultHandler handler) {
		new InputIpDialog(handler, defaultAddress, ui);
	}

	private final DialogResultHandler handler;

	public InputIpDialog(final DialogResultHandler handler, final InetAddress defaultAddress, final UI ui) {
		this.handler = handler;
		setModal(true);
		final PropertysetItem item = new PropertysetItem();
		final Property<InetAddress> property = new ObjectProperty<InetAddress>(defaultAddress, InetAddress.class);
		item.addItemProperty("address", property);
		final FieldGroup fieldGroup = new FieldGroup(item);
		fieldGroup.setFieldFactory(new FieldGroupFieldFactory() {

			@Override
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public <T extends Field> T createField(final Class<?> dataType, final Class<T> fieldType) {
				if (dataType.isAssignableFrom(InetAddress.class)) {
					final TextField textField = new TextField();
					textField.setConverter(new InetAddressConverter(InetAddress.class));
					return (T) textField;
				}
				return null;
			}
		});
		final FormLayout formLayout = new FormLayout();
		formLayout.addComponent(fieldGroup.buildAndBind("address"));
		final HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.setSizeFull();
		formLayout.addComponent(buttonLayout);
		buttonLayout.addComponent(new Button("Ok", new ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				try {
					fieldGroup.commit();
					final InetAddress value = property.getValue();
					handler.takeIp(value);
					ui.removeWindow(InputIpDialog.this);
				} catch (final CommitException e) {
					e.printStackTrace();
				}
			}
		}));
		buttonLayout.addComponent(new Button("Cancel", new ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				ui.removeWindow(InputIpDialog.this);
			}
		}));
		setContent(formLayout);
		ui.addWindow(this);
	}
}
