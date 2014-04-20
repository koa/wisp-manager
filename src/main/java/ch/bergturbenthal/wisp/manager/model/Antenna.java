package ch.bergturbenthal.wisp.manager.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@ToString(exclude = { "apBridge", "clientBridge" })
@EqualsAndHashCode(exclude = { "apBridge", "clientBridge" })
public class Antenna {
	private RangePair addresses;
	private String adminPassword;
	@OneToOne(mappedBy = "apAntenna")
	private Bridge apBridge;
	@OneToOne(mappedBy = "clientAntenna")
	private Bridge clientBridge;
	@OneToOne
	private NetworkDevice device;
	@Id
	@GeneratedValue
	private Long id;

	public String getTitle() {
		final boolean isAp;
		final Bridge bridge;
		if (apBridge != null) {
			isAp = true;
			bridge = apBridge;
		} else if (clientBridge != null) {
			isAp = false;
			bridge = clientBridge;
		} else {
			return "Unassigned";
		}

		return bridge.getTitle() + (isAp ? " AP" : " Client");
	}
}
