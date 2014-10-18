package ch.bergturbenthal.wisp.manager.view.component;

import java.util.Locale;

import ch.bergturbenthal.wisp.manager.model.IpAddress;
import ch.bergturbenthal.wisp.manager.model.IpNetwork;

import com.vaadin.data.fieldgroup.DefaultFieldGroupFieldFactory;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Field;
import com.vaadin.ui.TextField;

public class CustomFieldGroupFactory extends DefaultFieldGroupFieldFactory {

	private <T extends Field> AbstractTextField createAbstractTextStringField(final Class<T> fieldType) {
		if (fieldType.isAssignableFrom(AbstractField.class)) {
			return (AbstractTextField) super.createField(String.class, fieldType);
		}
		if (fieldType == Field.class) {
			return new TextField();
		}
		return null;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public <T extends Field> T createField(final Class<?> dataType, final Class<T> fieldType) {
		if (dataType.isAssignableFrom(IpAddress.class)) {
			final AbstractField<String> field = createStringField(fieldType);
			if (field != null) {
				field.setConverter(new IpAddressConverter());
				return (T) field;
			}
		}
		if (dataType.isAssignableFrom(IpNetwork.class)) {
			final AbstractField<String> field = createStringField(fieldType);
			if (field != null) {
				field.setConverter(new Converter<String, IpNetwork>() {

					@Override
					public IpNetwork convertToModel(final String value, final Class<? extends IpNetwork> targetType, final Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
						if (value == null || value.trim().isEmpty()) {
							return null;
						}
						return IpNetwork.resolveAddress(value);
					}

					@Override
					public String convertToPresentation(final IpNetwork value, final Class<? extends String> targetType, final Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
						if (value == null) {
							return "";
						}
						return value.getDescription();
					}

					@Override
					public Class<IpNetwork> getModelType() {
						return IpNetwork.class;
					}

					@Override
					public Class<String> getPresentationType() {
						return String.class;
					}
				});
				return (T) field;
			}

		}
		if (dataType == Integer.class) {
			final AbstractTextField textField = createAbstractTextStringField(fieldType);
			if (textField != null) {
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
		}
		return super.createField(dataType, fieldType);
	}

	private <T extends Field> AbstractField<String> createStringField(final Class<T> fieldType) {
		if (fieldType.isAssignableFrom(AbstractField.class)) {
			return (AbstractField<String>) super.createField(String.class, fieldType);
		}
		if (fieldType == Field.class) {
			return new TextField();
		}
		return null;
	}
}
