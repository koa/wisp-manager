package ch.bergturbenthal.wisp.manager.model;

import java.util.ArrayList;
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
import javax.persistence.OrderColumn;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(of = "id")
public class NetworkDevice {
	public static NetworkDevice createDevice(final NetworkDeviceModel model) {
		final NetworkDevice networkDevice = new NetworkDevice();
		networkDevice.setDeviceModel(model);

		final List<NetworkInterface> ifList = new ArrayList<>();
		for (final NetworkInterfaceType iface : model.getInterfaces()) {
			final NetworkInterface newIf = new NetworkInterface();
			newIf.setNetworkDevice(networkDevice);
			newIf.setType(iface);
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

	public String getTitle() {
		if (interfaces != null && !interfaces.isEmpty()) {
			return deviceModel + " - " + interfaces.get(0).getMacAddress().getAddress();
		}
		return id + " - " + deviceModel;
	}
}
