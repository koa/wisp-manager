package ch.bergturbenthal.wisp.manager.model;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import ch.bergturbenthal.wisp.manager.model.devices.NetworkInterfaceType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@EqualsAndHashCode(of = "id")
@ToString(exclude = "networkDevice")
public class NetworkInterface {
	@Id
	@GeneratedValue
	private Long id;
	// @Column(length = 12)
	private MacAddress macAddress;
	@ManyToOne(optional = false)
	private NetworkDevice networkDevice;
	@Enumerated(EnumType.STRING)
	private NetworkInterfaceType type;
}
