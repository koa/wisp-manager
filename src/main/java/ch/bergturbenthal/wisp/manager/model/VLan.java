package ch.bergturbenthal.wisp.manager.model;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Data;

@Data
@Entity
public class VLan {
	@Embedded
	private RangePair address;
	@Id
	@GeneratedValue
	private Long id;
	@ManyToOne
	private NetworkInterface networkInterface;
	@ManyToOne
	private Station station;
	private int vlanId;
}
