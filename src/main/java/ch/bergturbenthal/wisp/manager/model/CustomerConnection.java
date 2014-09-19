package ch.bergturbenthal.wisp.manager.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@EqualsAndHashCode(of = { "id", "ownNetworks" })
@ToString(exclude = { "station" })
public class CustomerConnection {
	@Id
	@GeneratedValue
	private Long id;
	private String name;
	@OneToOne(mappedBy = "customerConnection")
	private NetworkInterface networkInterface;
	@OneToMany(mappedBy = "customerConnection", orphanRemoval = true, cascade = CascadeType.ALL)
	private Set<VLan> ownNetworks = new HashSet<>();
	@ManyToOne
	private Station station;

}
