package ch.bergturbenthal.wisp.manager.model;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Version;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkInterfaceType;

@Data
@Entity
@EqualsAndHashCode(exclude = { "station", "antenna" })
@ToString(exclude = { "station", "antenna", "tunnelBegins", "tunnelEnds" })
public class NetworkDevice {
	public static NetworkDevice createDevice(final NetworkDeviceModel model) {
		return createDevice(model, null);
	}

	public static NetworkDevice createDevice(final NetworkDeviceModel model, final String baseMacAddress) {
		final NetworkDevice networkDevice = new NetworkDevice();
		networkDevice.setDeviceModel(model);

		final List<NetworkInterface> ifList = new ArrayList<>();
		final Iterator<MacAddress> macIterator = (baseMacAddress == null ? Collections.<MacAddress> emptyList() : model.getAddressIncrementorFactory()
																																																										.getAllMacAddresses(new MacAddress(baseMacAddress))).iterator();
		for (final NetworkInterfaceType iface : model.getInterfaces()) {
			final NetworkInterface newIf = new NetworkInterface();
			newIf.setNetworkDevice(networkDevice);
			newIf.setType(iface);
			if (macIterator.hasNext()) {
				newIf.setMacAddress(macIterator.next());
			}
			ifList.add(newIf);
		}
		networkDevice.setInterfaces(ifList);
		return networkDevice;
	}

	@OneToOne(mappedBy = "device")
	private Antenna antenna;

	private String currentPassword;
	@Enumerated(EnumType.STRING)
	@Column(updatable = false, nullable = false)
	private NetworkDeviceModel deviceModel;
	@ElementCollection
	@CollectionTable(name = "network_device_dns", joinColumns = @JoinColumn(name = "network_device"))
	private Set<IpAddress> dnsServers;
	@Id
	@GeneratedValue
	private Long id;
	@OrderColumn()
	@OneToMany(mappedBy = "networkDevice", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private List<NetworkInterface> interfaces;
	@Setter(AccessLevel.PROTECTED)
	private Long lastProvisionedAntennaVersion;
	@Setter(AccessLevel.PROTECTED)
	private Long lastProvisionedStationVersion;
	@Setter(AccessLevel.PROTECTED)
	private Long lastProvisionedVersion;
	@ElementCollection
	private Map<String, String> properties;
	@Column(unique = true, nullable = true)
	private String serialNumber;
	@OneToOne(mappedBy = "device")
	private Station station;
	@OneToMany(mappedBy = "startDevice", orphanRemoval = true)
	private List<IpIpv6Tunnel> tunnelBegins = new ArrayList<>();
	@OneToMany(mappedBy = "endDevice", orphanRemoval = true)
	private List<IpIpv6Tunnel> tunnelEnds = new ArrayList<>();
	@Column(columnDefinition = "numeric")
	private BigInteger v4AddressRaw;
	@Column(columnDefinition = "numeric")
	private BigInteger v6AddressRaw;
	@Version
	@Setter(AccessLevel.PROTECTED)
	private Long version;

	public String getTitle() {
		if (interfaces != null && !interfaces.isEmpty()) {
			final MacAddress macAddress = interfaces.get(0).getMacAddress();
			if (macAddress != null) {
				return deviceModel + " - " + macAddress.getAddress();
			}
		}
		return id + " - " + deviceModel;
	}

	public InetAddress getV4Address() {
		return IpAddress.bigInteger2InetAddress(v4AddressRaw);
	}

	public InetAddress getV6Address() {
		return IpAddress.bigInteger2InetAddress(v6AddressRaw);
	}

	public boolean isProvisioned() {
		if (lastProvisionedVersion == null) {
			return false;
		}
		if (version == null) {
			return false;
		}
		if (version.longValue() - lastProvisionedVersion.longValue() != 1) {
			return false;
		}
		if (station != null) {
			if (lastProvisionedStationVersion == null) {
				return false;
			}
			if (station.getVersion() == null) {
				return false;
			}
			if (station.getVersion().longValue() != lastProvisionedStationVersion.longValue()) {
				return false;
			}
		}
		if (antenna != null) {
			if (lastProvisionedAntennaVersion == null) {
				return false;
			}
			if (antenna.getVersion() == null) {
				return false;
			}
			if (antenna.getVersion().longValue() != lastProvisionedAntennaVersion.longValue()) {
				return false;
			}
		}
		return true;
	}

	public void setProvisioned() {
		lastProvisionedVersion = version;
		if (station != null) {
			lastProvisionedStationVersion = station.getVersion();
		}
		if (antenna != null) {
			lastProvisionedAntennaVersion = antenna.getVersion();
		}
	}

	public void setV4Address(final InetAddress host) {
		v4AddressRaw = IpAddress.inet2BigInteger(host);
	}

	public void setV6Address(final InetAddress host) {
		v6AddressRaw = IpAddress.inet2BigInteger(host);
	}

}
