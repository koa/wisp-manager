package ch.bergturbenthal.wisp.manager.model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(of = "id")
public class Station {
	@OneToMany(mappedBy = "startStation", orphanRemoval = true)
	private List<Connection> beginningConnections;
	@OneToMany(mappedBy = "endStation", orphanRemoval = true)
	private List<Connection> endingConnections;
	@Id
	@GeneratedValue
	private Long id;
	private String name;
	private Position position;

}
