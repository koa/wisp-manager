package ch.bergturbenthal.wisp.manager.view.component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

import ch.bergturbenthal.wisp.manager.model.IpAddress;

import com.vaadin.data.util.converter.Converter;
import com.vaadin.data.util.converter.Converter.ConversionException;

public class IpAddressConverter implements Converter<String, IpAddress> {
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
}