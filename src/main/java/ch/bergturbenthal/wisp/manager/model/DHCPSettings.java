package ch.bergturbenthal.wisp.manager.model;

import javax.persistence.Embeddable;

import lombok.Data;

@Embeddable
@Data
public class DHCPSettings {
	private Long endOffset;
	private Long leaseTime;
	private Long startOffset;
}
