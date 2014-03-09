package ch.bergturbenthal.wisp.manager.model;

import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class IpNetwork {
	private IpAddress address;
	private int netmask;

	@Override
	public String toString() {
		return address.getInetAddress() + "/" + netmask;
	}
}
