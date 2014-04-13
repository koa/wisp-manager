package ch.bergturbenthal.wisp.manager.view.component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

import com.vaadin.data.util.converter.Converter;

public final class InetAddressConverter<I extends InetAddress> implements Converter<String, I> {
	private static InetAddressConverter<InetAddress> instance = new InetAddressConverter<>(InetAddress.class);

	public static InetAddressConverter<InetAddress> getInstance() {
		return instance;
	}

	private final Class<I> type;

	public InetAddressConverter(final Class<I> type) {
		this.type = type;
	}

	@Override
	public I convertToModel(final String value, final Class<? extends I> targetType, final Locale locale) throws ConversionException {
		if (value == null) {
			return null;
		}
		try {
			return (I) InetAddress.getByName(value);
		} catch (final UnknownHostException e) {
			throw new ConversionException(e);
		}
	}

	@Override
	public String convertToPresentation(final I value, final Class<? extends String> targetType, final Locale locale) throws ConversionException {
		if (value == null) {
			return null;
		}
		return value.getHostAddress();
	}

	@Override
	public Class<I> getModelType() {
		return type;
	}

	@Override
	public Class<String> getPresentationType() {
		return String.class;
	}
}