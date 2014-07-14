package ch.bergturbenthal.wisp.manager.model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
// @Table(uniqueConstraints = @UniqueConstraint(columnNames = { "endStation", "startStation" }))
@EqualsAndHashCode(of = "id")
public class Connection {
	@OneToMany(mappedBy = "connection", orphanRemoval = true)
	@OrderColumn
	private List<Bridge> bridges;
	@ManyToOne
	private Station endStation;
	@Id
	@GeneratedValue
	private Long id;
	@ManyToOne
	private Station startStation;

	public String getTitle() {
		if (startStation != null && endStation != null) {
			return startStation.getName() + " - " + endStation.getName();
		}
		return toString();
	}
}
