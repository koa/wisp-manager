package ch.bergturbenthal.wisp.manager.view.component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

import ch.bergturbenthal.wisp.manager.model.IpAddress;

import com.vaadin.data.fieldgroup.DefaultFieldGroupFieldFactory;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.Field;
import com.vaadin.ui.TextField;

public class CustomFieldGroupFactory extends DefaultFieldGroupFieldFactory {

	@Override
	@SuppressWarnings("rawtypes")
	public <T extends Field> T createField(final Class<?> dataType, final Class<T> fieldType) {
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
		if (dataType == Integer.class) {
			final TextField textField = new TextField();
			textField.setConverter(new Converter<String, Integer>() {

				@Override
				public Integer convertToModel(final String value, final Class<? extends Integer> targetType, final Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
					if (value == null) {
						return null;
					}
					try {
						return Integer.valueOf(value);
					} catch (final NumberFormatException e) {
						throw new Converter.ConversionException("Cannot convert " + value, e);
					}
				}

				@Override
				public String convertToPresentation(final Integer value, final Class<? extends String> targetType, final Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
					if (value == null) {
						return null;
					}
					return value.toString();
				}

				@Override
				public Class<Integer> getModelType() {
					return Integer.class;
				}

				@Override
				public Class<String> getPresentationType() {
					return String.class;
				}
			});

			textField.addTextChangeListener(new TextChangeListener() {
				private String lastValue = null;

				@Override
				public void textChange(final TextChangeEvent event) {
					final String newText = event.getText();
					try {
						if (newText != null && newText.length() > 0) {
							Integer.parseInt(newText);
						}
						lastValue = newText;
					} catch (final NumberFormatException ex) {
						((TextField) event.getComponent()).setValue(lastValue);
					}
				}
			});
			textField.setNullRepresentation("");
			return (T) textField;
		}
		return super.createField(dataType, fieldType);
	}

}
