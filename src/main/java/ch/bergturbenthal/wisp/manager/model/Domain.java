package ch.bergturbenthal.wisp.manager.model;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@EqualsAndHashCode(exclude = "owningStations")
@ToString(exclude = "owningStations")
public class Domain {
	@ElementCollection
	private Collection<IpAddress> dnsServers = new ArrayList<IpAddress>();
	private String domainName;

	@Id
	@GeneratedValue
	private Long id;
	@OneToMany(mappedBy = "domain")
	private Collection<Station> owningStations = new ArrayList<>(0);
}
