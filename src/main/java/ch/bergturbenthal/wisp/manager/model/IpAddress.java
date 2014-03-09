package ch.bergturbenthal.wisp.manager.model;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class IpAddress {
	@Column(columnDefinition = "numeric")
	private BigInteger rawValue;

	public IpAddress(final InetAddress ipAddress) {
		final byte[] address = ipAddress.getAddress();
		rawValue = new BigInteger(address);
		// rawValue = ((address[0]) & 0xff) << 24 | ((address[1]) & 0xff) << 16 | ((address[2]) & 0xff) << 8 | ((address[3]) & 0xff);
	}

	public IpAddress(final long ipAddress) {
		rawValue = BigInteger.valueOf(ipAddress);
		// rawValue = (int) ipAddress;
	}

	public InetAddress getInetAddress() {
		try {
			return InetAddress.getByAddress(rawValue.toByteArray());
			// return (Inet4Address) Inet4Address.getByAddress(new byte[] { (byte) ((rawValue >> 24) & 0xff),
			// (byte) ((rawValue >> 16) & 0xff),
			// (byte) ((rawValue >> 8) & 0xff),
			// (byte) (rawValue & 0xff) });
		} catch (final UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return getInetAddress().toString();
	}
}
