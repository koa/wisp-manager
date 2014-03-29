package ch.bergturbenthal.wisp.manager.model;

import java.math.BigInteger;

import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class IpNetwork {
	private IpAddress address;
	private int netmask;

	public boolean containsAddress(final IpAddress otherAddress) {
		if (address.getAddressType() != otherAddress.getAddressType()) {
			return false;
		}
		final byte[] mask;
		switch (address.getAddressType()) {
		case V4:
			mask = new byte[4];
			break;
		case V6:
			mask = new byte[16];
			break;
		default:
			throw new IllegalArgumentException("Addresstype " + address.getAddressType() + " is onknown");
		}
		for (int i = 0; i < mask.length; i++) {
			final int byteMask = netmask - i * 8;
			if (byteMask > 7) {
				mask[i] = (byte) 0xff;
			} else if (byteMask < 1) {
				mask[i] = 0;
			} else {
				mask[i] = (byte) (0xff << (8 - byteMask));
			}
		}
		final BigInteger maskInteger = new BigInteger(mask);
		return address.getRawValue().and(maskInteger).equals(otherAddress.getRawValue().and(maskInteger));
	}

	@Override
	public String toString() {
		return address.getInetAddress() + "/" + netmask;
	}
}
