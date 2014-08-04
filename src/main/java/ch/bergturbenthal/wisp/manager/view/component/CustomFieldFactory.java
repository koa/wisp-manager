package ch.bergturbenthal.wisp.manager.view.component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ch.bergturbenthal.wisp.manager.model.IpAddress;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;

import com.vaadin.data.fieldgroup.DefaultFieldGroupFieldFactory;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.TextField;

@Slf4j
@AllArgsConstructor
public class CustomFieldFactory extends DefaultFieldGroupFieldFactory {
	private final CrudRepositoryContainer<NetworkDevice, Long> devicesContainer;

	@Override
	@SuppressWarnings("rawtypes")
	public <T extends Field> T createField(final Class<?> dataType, final Class<T> fieldType) {
		if (dataType.isAssignableFrom(NetworkDevice.class)) {
			final ComboBox comboBox = new ComboBox("Device", devicesContainer);
			comboBox.setNullSelectionAllowed(true);
			comboBox.setItemCaptionPropertyId("title");
			comboBox.setConverter(new Converter<Object, NetworkDevice>() {

				@Override
				public NetworkDevice convertToModel(final Object value, final Class<? extends NetworkDevice> targetType, final Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
					if (value == null) {
						return null;
					}
					return devicesContainer.getItem(value).getPojo();
				}

				@Override
				public Object convertToPresentation(final NetworkDevice value, final Class<? extends Object> targetType, final Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
					if (value == null) {
						return null;
					}
					return value.getId();
				}

				@Override
				public Class<NetworkDevice> getModelType() {
					return NetworkDevice.class;
				}

				@Override
				public Class<Object> getPresentationType() {
					// TODO Auto-generated method stub
					return Object.class;
				}
			});
			return (T) comboBox;
		}
		if (dataType.isAssignableFrom(IpAddress.class)) {
			final TextField textField = new TextField();
			textField.setConverter(new Converter<String, IpAddress>() {

				@Override
				public IpAddress convertToModel(final String value, final Class<? extends IpAddress> targetType, final Locale locale) throws Converter.ConversionException {
					if (value == null) {
						return null;
					}
					try {
						final InetAddress inetAddress = InetAddress.getByName(value);
						return new IpAddress(inetAddress);
					} catch (final UnknownHostException e) {
						throw new ConversionException("Cannot convert " + value + " to ip address", e);
					}
				}

				@Override
				public String convertToPresentation(final IpAddress value, final Class<? extends String> targetType, final Locale locale) throws Converter.ConversionException {
					if (value == null) {
						return null;
					}
					return value.getInetAddress().getHostAddress();
				}

				@Override
				public Class<IpAddress> getModelType() {
					return IpAddress.class;
				}

				@Override
				public Class<String> getPresentationType() {
					return String.class;
				}
			});
			return (T) textField;
		}
		return super.createField(dataType, fieldType);
	}
}