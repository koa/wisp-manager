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
@ToString(exclude = { "networkInterface", "customerConnection" })
@EqualsAndHashCode(of = { "id", "networkInterface", "vlanId" })
public class VLan {
	@Embedded
	private RangePair address;
	@ManyToOne
	private CustomerConnection customerConnection;
	@Id
	@GeneratedValue
	private Long id;
	@ManyToOne
	private NetworkInterface networkInterface;
	@ElementCollection
	@CollectionTable(name = "vlan_dns", joinColumns = @JoinColumn(name = "vlan"))
	private Set<IpAddress> privateDnsServers;
	private int vlanId;
}
