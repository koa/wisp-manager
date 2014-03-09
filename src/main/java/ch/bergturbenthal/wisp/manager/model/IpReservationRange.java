package ch.bergturbenthal.wisp.manager.model;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ch.bergturbenthal.wisp.manager.model.address.AddressRangeType;

@Data
@Entity
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@ToString(exclude = { "parentRange", "reservations" })
public class IpReservationRange {
	private String comment;
	@Id
	@GeneratedValue
	private Long id;
	@ManyToOne
	private IpReservationRange parentRange;
	private IpNetwork range;
	private int rangeMask;
	@OneToMany(mappedBy = "parentRange")
	private Collection<IpReservationRange> reservations = new ArrayList<>(0);
	@NotNull
	@Enumerated(EnumType.STRING)
	private AddressRangeType type;

	public IpReservationRange(final IpNetwork range, final int rangeMask, final AddressRangeType rangeType) {
		this.range = range;
		this.rangeMask = rangeMask;
		type = rangeType;
	}

	@Min(1)
	public int getAvailableReservations() {
		final int availableBitCount = range.getNetmask() - rangeMask;
		return 1 << availableBitCount;
	}

}
