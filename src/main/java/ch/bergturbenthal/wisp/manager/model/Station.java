package ch.bergturbenthal.wisp.manager.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class Station {
	@Id
	@GeneratedValue
	private Long id;
	private String name;
	private Position position;
}
