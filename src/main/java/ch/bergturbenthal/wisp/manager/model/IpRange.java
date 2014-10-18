package ch.bergturbenthal.wisp.manager.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ch.bergturbenthal.wisp.manager.model.address.AddressRangeType;

@Data
@Entity
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@ToString(exclude = { "parentRange",
											"reservations",
											"v4OwningAntenna",
											"v4OwningAutoConnectionPorts",
											"v4OwningGatewaySettings",
											"v4OwningStations",
											"v4OwningTunnels",
											"v4OwningVlans",
											"v6OwningAntenna",
											"v6OwningAutoConnectionPorts",
											"v6OwningGatewaySettings",
											"v6OwningStations",
											"v6OwningVlans" })
// @Table(indexes = { @Index(columnList = "range.address, rangeMask", unique = true) })
public class IpRange {
	private String comment;
	@Id
	@GeneratedValue
	private Long id;
	@ManyToOne
	private IpRange parentRange;
	private IpNetwork range;
	private int rangeMask;
	@OneToMany(mappedBy = "parentRange", cascade = CascadeType.ALL)
	private Collection<IpRange> reservations = new ArrayList<>(0);
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private AddressRangeType type;

	@OneToMany(mappedBy = "addresses.v4Address")
	private Collection<Antenna> v4OwningAntenna = new ArrayList<Antenna>(0);
	@OneToMany(mappedBy = "portAddress.v4Address")
	private Collection<AutoConnectionPort> v4OwningAutoConnectionPorts = new ArrayList<AutoConnectionPort>(0);
	@OneToMany(mappedBy = "managementAddress.v4Address")
	private Collection<GatewaySettings> v4OwningGatewaySettings = new ArrayList<GatewaySettings>(0);
	@OneToMany(mappedBy = "loopback.v4Address")
	private Collection<Station> v4OwningStations = new ArrayList<Station>(0);
	@OneToMany(mappedBy = "v4Address")
	private Collection<IpIpv6Tunnel> v4OwningTunnels = new ArrayList<IpIpv6Tunnel>(0);
	@OneToMany(mappedBy = "address.v4Address")
	private Collection<VLan> v4OwningVlans = new ArrayList<VLan>(0);

	@OneToMany(mappedBy = "addresses.v6Address")
	private Collection<Antenna> v6OwningAntenna = new ArrayList<Antenna>(0);
	@OneToMany(mappedBy = "portAddress.v6Address")
	private Collection<AutoConnectionPort> v6OwningAutoConnectionPorts = new ArrayList<AutoConnectionPort>(0);
	@OneToMany(mappedBy = "managementAddress.v6Address")
	private Collection<GatewaySettings> v6OwningGatewaySettings = new ArrayList<GatewaySettings>(0);
	@OneToMany(mappedBy = "loopback.v6Address")
	private Collection<Station> v6OwningStations = new ArrayList<Station>(0);
	@OneToMany(mappedBy = "address.v6Address")
	private Collection<VLan> v6OwningVlans = new ArrayList<VLan>(0);

	public IpRange(final IpNetwork range, final int rangeMask, final AddressRangeType rangeType) {
		this.range = range;
		this.rangeMask = rangeMask;
		type = rangeType;
	}

	public long getAvailableReservations() {
		final int availableBitCount = rangeMask - range.getNetmask();
		if (availableBitCount > 60) {
			return Long.MAX_VALUE;
		}
		return 1 << availableBitCount;
	}

	public Collection<Antenna> getOwningAntennas() {
		return getV4OrV6(v4OwningAntenna, v6OwningAntenna);
	}

	public Collection<AutoConnectionPort> getOwningAutoConnectionPorts() {
		return getV4OrV6(v4OwningAutoConnectionPorts, v6OwningAutoConnectionPorts);
	}

	public Collection<GatewaySettings> getOwningGatewaySettings() {
		return getV4OrV6(v4OwningGatewaySettings, v6OwningGatewaySettings);
	}

	public Collection<Station> getOwningStations() {
		return getV4OrV6(v4OwningStations, v6OwningStations);
	}

	public Collection<IpIpv6Tunnel> getOwningTunnels() {
		return getV4OrV6(v4OwningTunnels, Collections.<IpIpv6Tunnel> emptyList());
	}

	public Collection<VLan> getOwningVlans() {
		return getV4OrV6(v4OwningVlans, v6OwningVlans);
	}

	private <T> Collection<T> getV4OrV6(final Collection<T> v4Collection, final Collection<T> v6Collection) {
		switch (range.getAddress().getAddressType()) {
		case V4:
			return v4Collection;
		case V6:
			return v6Collection;
		}
		throw new IllegalArgumentException("Address-Type " + range.getAddress().getAddressType() + " unsupported");
	}

	public boolean isOrphan() {
		return getReservations().isEmpty() && getOwningAntennas().isEmpty()
						&& getOwningGatewaySettings().isEmpty()
						&& getOwningStations().isEmpty()
						&& getOwningVlans().isEmpty()
						&& getOwningAutoConnectionPorts().isEmpty()
						&& getOwningTunnels().isEmpty();
	}

}
