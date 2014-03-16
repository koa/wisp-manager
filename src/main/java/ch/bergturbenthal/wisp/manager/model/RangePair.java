package ch.bergturbenthal.wisp.manager.model;

import java.net.InetAddress;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToOne;

import lombok.Data;

@Data
@Embeddable
public class RangePair {
	@OneToOne(cascade = CascadeType.ALL)
	private IpRange v4Address;
	@OneToOne(cascade = CascadeType.ALL)
	private IpRange v6Address;

	public InetAddress getInet4Address() {
		if (v4Address == null) {
			return null;
		}
		final IpNetwork range = v4Address.getRange();
		if (range == null) {
			return null;
		}
		final IpAddress address = range.getAddress();
		if (address == null) {
			return null;
		}
		return address.getInetAddress();
	}

	public int getInet4Mask() {
		if (v4Address == null) {
			return -1;
		}
		return v4Address.getRange().getNetmask();
	}

	public int getInet4ParentMask() {
		if (v4Address == null) {
			return -1;
		}
		return v4Address.getParentRange().getRange().getNetmask();
	}

	public InetAddress getInet6Address() {
		if (v6Address == null) {
			return null;
		}
		final IpNetwork range = v6Address.getRange();
		if (range == null) {
			return null;
		}
		final IpAddress address = range.getAddress();
		if (address == null) {
			return null;
		}
		return address.getInetAddress();
	}

	public int getInet6Mask() {
		if (v6Address == null) {
			return -1;
		}
		return v6Address.getRange().getNetmask();
	}

	public int getInet6ParentMask() {
		if (v6Address == null) {
			return -1;
		}
		return v6Address.getParentRange().getRange().getNetmask();
	}
}
