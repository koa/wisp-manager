package ch.bergturbenthal.wisp.manager.model.address;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IpAddressType {
	V4(32),
	V6(128);
	private final int bitCount;

	public int getByteCount() {
		return bitCount << 3;
	}
}
