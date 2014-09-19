package ch.bergturbenthal.wisp.manager.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@ToString(exclude = { "station" })
@EqualsAndHashCode(exclude = { "station" })
public class AutoConnectionPort {
	@Id
	@GeneratedValue
	private Long id;
	@OneToOne(mappedBy = "autoConnectionPort")
	private NetworkInterface networkInterface;
	private RangePair portAddress;
	@ManyToOne
	private Station station;
}
