package ch.bergturbenthal.wisp.manager.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(of = "id")
public class Station {
	@OneToMany(mappedBy = "startStation", orphanRemoval = true)
	private List<Connection> beginningConnections;
	@OneToOne(cascade = CascadeType.ALL)
	private NetworkDevice device;
	@OneToMany(mappedBy = "endStation", orphanRemoval = true)
	private List<Connection> endingConnections;
	@Id
	@GeneratedValue
	private Long id;
	private RangePair loopback;
	private String name;
	@OneToMany(mappedBy = "station", orphanRemoval = true, cascade = CascadeType.ALL)
	private Set<VLan> ownNetworks = new HashSet();
	private Position position;

}
