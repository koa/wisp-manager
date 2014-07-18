package ch.bergturbenthal.wisp.manager.model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import lombok.Data;

@Data
@Entity
// @EqualsAndHashCode(of = "id")
public class IpIpv6Tunnel {
	@ManyToOne
	private NetworkDevice endDevice;
	@Id
	@GeneratedValue
	private Long id;
	@ManyToOne
	private NetworkDevice startDevice;
	@OneToOne(cascade = CascadeType.ALL)
	private IpRange v4Address;
}
