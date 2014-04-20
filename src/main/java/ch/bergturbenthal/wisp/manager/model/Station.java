package ch.bergturbenthal.wisp.manager.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
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
	private String adminPassword;
	@OneToMany(mappedBy = "startStation", orphanRemoval = true)
	private List<Connection> beginningConnections = new ArrayList<>();
	@OneToOne(cascade = CascadeType.ALL)
	private NetworkDevice device;
	@OneToMany(mappedBy = "endStation", orphanRemoval = true)
	private List<Connection> endingConnections = new ArrayList<>();
	@Id
	@GeneratedValue
	private Long id;
	private RangePair loopback;
	private String name;
	@OneToMany(mappedBy = "station", orphanRemoval = true, cascade = CascadeType.ALL)
	private Set<VLan> ownNetworks = new HashSet<>();
	private Position position;
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
