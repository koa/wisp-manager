package ch.bergturbenthal.wisp.manager.model;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class GlobalDnsServer {
	@Id
	private IpAddress address;
}
