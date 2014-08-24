package ch.bergturbenthal.wisp.manager.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@ToString(exclude = { "networkInterface", "customerConnection" })
@EqualsAndHashCode(of = { "id", "networkInterface", "vlanId" })
public class VLan {
	public static Collection<VLan> sortVLans(final Collection<VLan> unsorted) {
		final List<VLan> networkList = new ArrayList<VLan>(unsorted);
		Collections.sort(networkList, new Comparator<VLan>() {

			@Override
			public int compare(final VLan o1, final VLan o2) {
				return o1.getVlanId() - o2.getVlanId();
			}
		});
		return networkList;

	}

	@Embedded
	private RangePair address;
	@ManyToOne
	private CustomerConnection customerConnection;
	@Embedded
	private DHCPSettings dhcpSettings;
	@Id
	@GeneratedValue
	private Long id;
	@ManyToOne
	private NetworkInterface networkInterface;
	@ElementCollection
	@CollectionTable(name = "vlan_dns", joinColumns = @JoinColumn(name = "vlan"))
	private Set<IpAddress> privateDnsServers;
	private int vlanId;
}
