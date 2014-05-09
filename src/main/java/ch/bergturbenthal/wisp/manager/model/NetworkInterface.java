package ch.bergturbenthal.wisp.manager.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkInterfaceType;

@Data
@Entity
@EqualsAndHashCode(of = "id")
@ToString(exclude = "networkDevice")
public class NetworkInterface {
	@Id
	@GeneratedValue
	private Long id;
	private String interfaceName;
	// @Column(unique = true, nullable = true)
	private MacAddress macAddress;
	@ManyToOne(optional = false)
	private NetworkDevice networkDevice;
	@OneToMany(mappedBy = "networkInterface", orphanRemoval = true, cascade = CascadeType.ALL)
	private Set<VLan> networks = new HashSet<VLan>();
	@Enumerated(EnumType.STRING)
	private NetworkInterfaceRole role;
	@Enumerated(EnumType.STRING)
	private NetworkInterfaceType type;
}
