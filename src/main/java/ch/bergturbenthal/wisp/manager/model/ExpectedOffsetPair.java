package ch.bergturbenthal.wisp.manager.model;

import java.math.BigInteger;

import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ch.bergturbenthal.wisp.manager.model.address.IpAddressType;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ExpectedOffsetPair {
	private Integer expectedV4Offset;
	private BigInteger expectedV6Offset;

	public BigInteger getExpectedOffset(final IpAddressType type) {
		switch (type) {
		case V4:
			if (expectedV4Offset == null) {
				return null;
			}
			return BigInteger.valueOf(expectedV4Offset.longValue());
		case V6:
			return expectedV6Offset;
		default:
			return null;
		}
	}

	public void setExpectedOffset(final BigInteger value, final IpAddressType type) {
		switch (type) {
		case V4:
			if (value == null) {
				expectedV4Offset = null;
			} else {
				expectedV4Offset = Integer.valueOf(value.intValue());
			}
			break;
		case V6:
			expectedV6Offset = value;
			break;
		}
	}
}
