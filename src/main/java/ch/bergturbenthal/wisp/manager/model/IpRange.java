package ch.bergturbenthal.wisp.manager.model;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

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
// @Table(indexes = { @Index(columnList = "range.address, rangeMask", unique = true) })
public class IpRange {
	private String comment;
	@Id
	@GeneratedValue
	private Long id;
	@ManyToOne
	private IpRange parentRange;
	private IpNetwork range;
	private int rangeMask;
	@OneToMany(mappedBy = "parentRange", cascade = CascadeType.ALL)
	private Collection<IpRange> reservations = new ArrayList<>(0);
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private AddressRangeType type;

	public IpRange(final IpNetwork range, final int rangeMask, final AddressRangeType rangeType) {
		this.range = range;
		this.rangeMask = rangeMask;
		type = rangeType;
	}

	public long getAvailableReservations() {
		final int availableBitCount = rangeMask - range.getNetmask();
		if (availableBitCount > 60) {
			return Long.MAX_VALUE;
		}
		return 1 << availableBitCount;
	}

}
