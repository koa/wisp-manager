package ch.bergturbenthal.wisp.manager.model;

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
}
