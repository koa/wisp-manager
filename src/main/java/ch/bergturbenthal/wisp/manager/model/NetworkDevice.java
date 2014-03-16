package ch.bergturbenthal.wisp.manager.model;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkInterfaceType;

@Data
@Entity
@EqualsAndHashCode(of = "id")
@ToString(exclude = "station")
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

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(updatable = false)
	private NetworkDeviceModel deviceModel;
	@Id
	@GeneratedValue
	private Long id;
	@OrderColumn()
	@OneToMany(mappedBy = "networkDevice", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private List<NetworkInterface> interfaces;
	@Column(unique = true, nullable = true)
	private String serialNumber;

	@OneToOne(mappedBy = "device")
	private Station station;
	@Column(columnDefinition = "numeric")
	private BigInteger v4AddressRaw;
	@Column(columnDefinition = "numeric")
	private BigInteger v6AddressRaw;

	public String getTitle() {
		if (interfaces != null && !interfaces.isEmpty()) {
			final MacAddress macAddress = interfaces.get(0).getMacAddress();
			if (macAddress != null) {
				return deviceModel + " - " + macAddress.getAddress();
			}
		}
		return id + " - " + deviceModel;
	}

	public void setV4Address(final Inet4Address host) {
		v4AddressRaw = IpAddress.inet2BigInteger(host);
	}

	public void setV6Address(final Inet6Address host) {
		v6AddressRaw = IpAddress.inet2BigInteger(host);
	}

}
