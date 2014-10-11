package ch.bergturbenthal.wisp.manager.model;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.persistence.Embeddable;

import lombok.Data;
import lombok.NoArgsConstructor;
import ch.bergturbenthal.wisp.manager.model.address.IpAddressType;

@Data
@Embeddable
@NoArgsConstructor
public class IpNetwork {
	private static byte[] calculateNetmask(final IpAddressType addressType, final int netmaskLength) {
		final byte[] mask;
		switch (addressType) {
		case V4:
			mask = new byte[4];
			break;
		case V6:
			mask = new byte[16];
			break;
		default:
			throw new IllegalArgumentException("Addresstype " + addressType + " is onknown");
		}
		for (int i = 0; i < mask.length; i++) {
			final int byteMask = netmaskLength - i * 8;
			if (byteMask > 7) {
				mask[i] = (byte) 0xff;
			} else if (byteMask < 1) {
				mask[i] = 0;
			} else {
				mask[i] = (byte) (0xff << (8 - byteMask));
			}
		}
		return mask;
	}

	private IpAddress address;

	private int netmask;

	public IpNetwork(final IpAddress ipAddress, final int addressMask) {
		netmask = addressMask;
		final BigInteger maskInteger = new BigInteger(calculateNetmask(ipAddress.getAddressType(), addressMask));
		final BigInteger rawValue = ipAddress.getRawValue();
		final BigInteger maskedAddress = rawValue.and(maskInteger);
		if (rawValue.equals(maskedAddress)) {
			address = ipAddress;
		} else {
			address = new IpAddress(maskedAddress);
		}
	}

	public boolean containsAddress(final IpAddress otherAddress) {
		if (address.getAddressType() != otherAddress.getAddressType()) {
			return false;
		}
		final BigInteger maskInteger = new BigInteger(getByteMask());
		final BigInteger maskedAddress = address.getRawValue().and(maskInteger);
		return maskedAddress.equals(otherAddress.getRawValue().and(maskInteger));
	}

	private byte[] getByteMask() {
		final int netmaskLength = netmask;
		final IpAddressType addressType = address.getAddressType();
		return calculateNetmask(addressType, netmaskLength);
	}

	public String getDescription() {
		return address.getInetAddress().getHostAddress() + "/" + netmask;
	}

	public InetAddress getNetmaskAsAddress() {
		try {
			return InetAddress.getByAddress(getByteMask());
		} catch (final UnknownHostException e) {
			throw new RuntimeException("Cannot generate bitwise mask for /" + netmask, e);
		}
	}

	@Override
	public String toString() {
		return address.getInetAddress() + "/" + netmask;
	}
}
