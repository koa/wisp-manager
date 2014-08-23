package ch.bergturbenthal.wisp.manager.model;

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
	private String gatewayName;
	private GatewayType gatewayType;
	private boolean hasIPv4;
	private boolean hasIPv6;
	@Id
	@GeneratedValue
	private Long id;
	@Embedded
	private RangePair managementAddress;
	@Embedded
	private ExpectedOffsetPair managementExpectedOffsetPair;
	private String password;
	@ManyToOne(optional = false)
	private Station station;
	private String userName;
}
