package ch.bergturbenthal.wisp.manager.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@EqualsAndHashCode(of = "id")
@ToString(exclude = "vlan")
public class PortExpose {
	@Id
	@GeneratedValue
	private Long id;
	private int portNumber;
	private IpAddress targetAddress;
	@ManyToOne
	private VLan vlan;
}
