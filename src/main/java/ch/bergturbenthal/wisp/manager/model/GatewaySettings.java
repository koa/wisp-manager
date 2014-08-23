package ch.bergturbenthal.wisp.manager.model;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@EqualsAndHashCode(exclude = "station")
@ToString(exclude = "station")
public class GatewaySettings {
	@Embedded
	private ExpectedOffsetPair expectedOffsetPair;
	private String gatewayName;
	private GatewayType gatewayType;
	private boolean hasIPv4;
	private boolean hasIPv6;
	@Id
	@GeneratedValue
	private Long id;
	private String password;
	@ManyToOne(optional = false)
	private Station station;
	private String userName;
	@Embedded
	@AttributeOverrides({ @AttributeOverride(name = "address.addressType", column = @Column(name = "v4_address_type")),
												@AttributeOverride(name = "address.rawValue", column = @Column(name = "v4_raw_value")),
												@AttributeOverride(name = "netmask", column = @Column(name = "v4_mask")) })
	private IpNetwork v4Address;
	@Embedded
	@AttributeOverrides({ @AttributeOverride(name = "address.addressType", column = @Column(name = "v6_address_type")),
												@AttributeOverride(name = "address.rawValue", column = @Column(name = "v6_raw_value")),
												@AttributeOverride(name = "netmask", column = @Column(name = "v6_mask")) })
	private IpNetwork v6Address;
}
