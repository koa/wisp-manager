package ch.bergturbenthal.wisp.manager.model;

import javax.persistence.Embeddable;

import lombok.Data;

@Embeddable
@Data
public class DHCPSettings {
	private Long addressCount;
	private Long leaseTime;
	private Long startOffset;
}
