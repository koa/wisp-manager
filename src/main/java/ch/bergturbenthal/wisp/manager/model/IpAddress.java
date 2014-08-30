package ch.bergturbenthal.wisp.manager.model;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ch.bergturbenthal.wisp.manager.model.address.IpAddressType;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class IpAddress implements Serializable {
	public static InetAddress bigInteger2InetAddress(final BigInteger rawValue) {
		if (rawValue == null) {
			return null;
		}
		try {
			return InetAddress.getByAddress(rawValue.toByteArray());
		} catch (final UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	public static BigInteger inet2BigInteger(final InetAddress ipAddress) {
		if (ipAddress == null) {
			return null;
		}
		final byte[] address = ipAddress.getAddress();
		return new BigInteger(address);
	}

	@Enumerated(EnumType.STRING)
	private IpAddressType addressType;

	@Column(precision = 45)
	private BigInteger rawValue;

	public IpAddress(final BigInteger ipAddress) {
		rawValue = ipAddress;
		if (getInetAddress() instanceof Inet4Address) {
			addressType = IpAddressType.V4;
		} else {
			addressType = IpAddressType.V6;
		}
	}

	public IpAddress(final InetAddress ipAddress) {
		rawValue = inet2BigInteger(ipAddress);
		if (ipAddress instanceof Inet4Address) {
			addressType = IpAddressType.V4;
		} else {
			addressType = IpAddressType.V6;
		}
	}

	public InetAddress getAddressOfNetwork(final long addressIndex) {
		if (rawValue == null) {
			return null;
		}
		try {
			return InetAddress.getByAddress(rawValue.add(BigInteger.valueOf(addressIndex)).toByteArray());
		} catch (final UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	public InetAddress getInetAddress() {
		return bigInteger2InetAddress(rawValue);
	}

	public IpAddress mask(final int maskLength) {
		final int length;
		switch (addressType) {
		case V4:
			length = 32;
			break;
		case V6:
			length = 128;
			break;
		default:
			throw new IllegalStateException("Unsupported Address-Type " + addressType);
		}
		final int bitClearCount = length - maskLength;
		final BigInteger mask = BigInteger.ONE.shiftLeft(maskLength).subtract(BigInteger.ONE).shiftLeft(bitClearCount);
		final BigInteger maskedAddress = rawValue.and(mask);
		if (maskedAddress.equals(rawValue)) {
			return this;
		}
		return new IpAddress(maskedAddress);
	}

	@Override
	public String toString() {
		return String.valueOf(getInetAddress());
	}
}
