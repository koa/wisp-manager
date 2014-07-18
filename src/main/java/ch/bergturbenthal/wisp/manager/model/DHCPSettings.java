package ch.bergturbenthal.wisp.manager.model;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

import lombok.Data;

@Embeddable
@Data
public class DHCPSettings {
	@Embedded
	@AttributeOverrides({ @AttributeOverride(name = "addressType", column = @Column(name = "dhcp_end_address_type")),
												@AttributeOverride(name = "rawValue", column = @Column(name = "dhcp_end_raw_value")) })
	private IpAddress endIp;
	private Long leaseTime;
	@Embedded
	@AttributeOverrides({ @AttributeOverride(name = "addressType", column = @Column(name = "dhcp_start_address_type")),
												@AttributeOverride(name = "rawValue", column = @Column(name = "dhcp_start_raw_value")) })
	private IpAddress startIp;
}
