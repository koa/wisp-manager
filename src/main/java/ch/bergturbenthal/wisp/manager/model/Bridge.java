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
@EqualsAndHashCode(exclude = "connection")
@ToString(exclude = "connection")
public class Bridge {
	@OneToOne(orphanRemoval = true)
	private Antenna apAntenna;
	@OneToOne(orphanRemoval = true)
	private Antenna clientAntenna;
	@ManyToOne
	private Connection connection;
	@Id
	@GeneratedValue
	private Long id;
	private String wpa2Key;

	public String getTitle() {
		final int bridgeIndex = connection.getBridges().indexOf(this);
		return connection.getTitle() + "[" + bridgeIndex + "]";
	}

}
