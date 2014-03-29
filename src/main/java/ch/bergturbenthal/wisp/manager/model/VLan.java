package ch.bergturbenthal.wisp.manager.model;

import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@ToString(exclude = { "networkInterface", "station" })
@EqualsAndHashCode(of = { "id", "station", "networkInterface", "vlanId" })
public class VLan {
	@Embedded
	private RangePair address;
	@Id
	@GeneratedValue
	private Long id;
	@ManyToOne
	private NetworkInterface networkInterface;
	@ElementCollection
	@CollectionTable(name = "vlan_dns", joinColumns = @JoinColumn(name = "vlan"))
	private Set<IpAddress> privateDnsServers;
	@ManyToOne
	private Station station;
	private int vlanId;
}
