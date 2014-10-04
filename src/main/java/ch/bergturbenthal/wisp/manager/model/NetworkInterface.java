package ch.bergturbenthal.wisp.manager.model;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkInterfaceType;

@Data
@Entity
@EqualsAndHashCode(of = "id")
@ToString(exclude = { "networkDevice", "gatewaySettings", "customerConnection", "autoConnectionPort" })
public class NetworkInterface {
	@OneToOne
	private AutoConnectionPort autoConnectionPort;
	@OneToOne
	private CustomerConnection customerConnection;
	@OneToOne
	private GatewaySettings gatewaySettings;
	@Id
	@GeneratedValue
	private Long id;
	// @Column(unique = true, nullable = true)
	private MacAddress macAddress;
	@ManyToOne(optional = false)
	private NetworkDevice networkDevice;

	@Enumerated(EnumType.STRING)
	private NetworkInterfaceRole role;
	@Enumerated(EnumType.STRING)
	private NetworkInterfaceType type;

	public String getInterfaceName() {
		if (customerConnection != null) {
			return customerConnection.getName();
		}
		if (gatewaySettings != null) {
			return gatewaySettings.getGatewayName();
		}
		return null;
	}
}
