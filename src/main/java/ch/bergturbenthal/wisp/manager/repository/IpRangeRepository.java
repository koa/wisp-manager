package ch.bergturbenthal.wisp.manager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.model.address.AddressRangeType;
import ch.bergturbenthal.wisp.manager.model.address.IpAddressType;

public interface IpRangeRepository extends CrudRepository<IpRange, Long> {
	@Query("select r from IpRange r where type=?1 and r.range.address.addressType=?2 and r.rangeMask<=?3 order by r.rangeMask desc")
	List<IpRange> findMatchingRange(final AddressRangeType rangeType, final IpAddressType addressType, final int maxNetSize);

	@Query("select r from IpRange r where type='ROOT'")
	List<IpRange> findAllRootRanges();
}
