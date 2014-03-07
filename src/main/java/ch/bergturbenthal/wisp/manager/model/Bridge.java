package ch.bergturbenthal.wisp.manager.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Data;

@Entity
@Data
public class Bridge {
	@ManyToOne
	private Connection connection;
	@Id
	@GeneratedValue
	private Long id;
	private String wpa2Key;
}
