package ch.bergturbenthal.wisp.manager.model;

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
public class IpAddress {
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

	@Column(columnDefinition = "numeric")
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

	@Override
	public String toString() {
		return String.valueOf(getInetAddress());
	}
}
