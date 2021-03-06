package ch.bergturbenthal.wisp.manager.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Version;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

@Data
@Entity
@EqualsAndHashCode(of = "id")
@ToString(exclude = { "beginningConnections", "endingConnections" })
public class Station {
	@OneToMany(mappedBy = "station", orphanRemoval = true, cascade = CascadeType.ALL)
	private List<AutoConnectionPort> autoConnectionPorts = new ArrayList<AutoConnectionPort>();
	@OneToMany(mappedBy = "startStation", orphanRemoval = true)
	private List<Connection> beginningConnections = new ArrayList<>();
	@OneToMany(mappedBy = "station", orphanRemoval = true, cascade = CascadeType.ALL)
	private Set<CustomerConnection> customerConnections = new HashSet<CustomerConnection>();
	@OneToOne(cascade = CascadeType.ALL)
	private NetworkDevice device;
	@ManyToOne
	private Domain domain;
	@OneToMany(mappedBy = "endStation", orphanRemoval = true)
	private List<Connection> endingConnections = new ArrayList<>();
	@OneToMany(mappedBy = "station", orphanRemoval = true, cascade = CascadeType.ALL)
	private List<GatewaySettings> gatewaySettings = new ArrayList<GatewaySettings>();
	@Id
	@GeneratedValue
	private Long id;
	private RangePair loopback;
	private String name;
	private Position position;
	@OneToMany(mappedBy = "startStation", orphanRemoval = true)
	private List<IpIpv6Tunnel> tunnelBegins = new ArrayList<>();
	private boolean tunnelConnection;

	@OneToMany(mappedBy = "endStation", orphanRemoval = true)
	private List<IpIpv6Tunnel> tunnelEnds = new ArrayList<>();
	@Version
	@Setter(AccessLevel.PROTECTED)
	private Long version;

	public List<Connection> getConnections() {
		final List<Connection> ret = new ArrayList<Connection>(beginningConnections.size() + endingConnections.size());
		ret.addAll(beginningConnections);
		ret.addAll(endingConnections);
		return ret;
	}

	public String getLoopbackDescription() {
		if (loopback == null) {
			return null;
		}
		final StringBuilder stringBuilder = new StringBuilder();
		if (loopback.getInet4Address() != null) {
			stringBuilder.append(loopback.getInet4Address().getHostAddress());
		}
		stringBuilder.append(":");
		if (loopback.getInet6Address() != null) {
			stringBuilder.append(loopback.getInet6Address().getHostAddress());
		}
		return stringBuilder.toString();
	}

}
