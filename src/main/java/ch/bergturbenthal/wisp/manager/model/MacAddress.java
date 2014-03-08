package ch.bergturbenthal.wisp.manager.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@NoArgsConstructor
public class MacAddress {
	private static final Pattern IEEE_802_PATTERN = Pattern.compile("([0-9a-fA-F]{1,2}[-:]){5}([0-9a-fA-F]{1,2})");
	private static final Pattern IEEE_802_SPLIT_PATTERN = Pattern.compile("[-:]");
	@Column(length = 17)
	private String address;

	public MacAddress(final String addressAsString) {
		final byte[] rawAddress = parseAddress(addressAsString);
		address = formatAddress(rawAddress);
	}

	private long convertLong(final byte[] rawAddress) {
		long ret = 0;
		for (int i = 0; i < 6; i++) {
			ret |= (long) (rawAddress[i] & 0xff) << (5 - i) * 8;
		}
		return ret;
	}

	private byte[] decodeLong(final long value) {
		final byte[] ret = new byte[6];
		for (int i = 0; i < 6; i++) {
			ret[i] = (byte) (value >> ((5 - i) * 8));
		}
		return ret;
	}

	private String formatAddress(final byte[] address2) {
		final StringBuilder ret = new StringBuilder();
		for (final byte element : address2) {
			final String hexString = Integer.toHexString((element) & 0xff);
			if (ret.length() > 0) {
				ret.append(':');
			}
			if (hexString.length() == 1) {
				ret.append('0');
			}
			ret.append(hexString);
		}
		return ret.toString();
	}

	public MacAddress nextAddress() {
		return offsetAddress(1);
	}

	public MacAddress offsetAddress(final long increment) {
		final byte[] parsedAddress = parseAddress(getAddress());
		final long longAddress = convertLong(parsedAddress);
		final byte[] composedLong = decodeLong(longAddress + increment);
		return new MacAddress(formatAddress(composedLong));
	}

	private byte[] parseAddress(final String addressAsString) {
		final byte[] rawAddress = new byte[6];
		final Matcher ieee802Matcher = IEEE_802_PATTERN.matcher(addressAsString);
		if (ieee802Matcher.matches()) {
			final String[] comps = IEEE_802_SPLIT_PATTERN.split(addressAsString);
			for (int i = 0; i < 6; i++) {
				rawAddress[i] = (byte) Integer.parseInt(comps[i], 16);
			}
		} else {
			final String lowerString = addressAsString.toLowerCase();
			final StringBuilder rawString = new StringBuilder();
			for (int i = 0; i < lowerString.length(); i++) {
				final char nextChar = lowerString.charAt(i);
				if (Character.isLetterOrDigit(nextChar) && nextChar <= 'f') {
					rawString.append(nextChar);
				}
			}
			if (rawString.length() != 12) {
				throw new IllegalArgumentException("Address " + addressAsString + " is not valid");
			}
			for (int i = 0; i < 6; i++) {
				rawAddress[i] = (byte) Integer.parseInt(rawString.substring(i * 2, i * 2 + 2), 16);
			}
		}
		return rawAddress;
	}

}
