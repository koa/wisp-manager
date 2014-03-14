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
		return v4Address.getRange().getAddress().getInetAddress();
	}

	public InetAddress getInet6Address() {
		return v6Address.getRange().getAddress().getInetAddress();
	}
}
