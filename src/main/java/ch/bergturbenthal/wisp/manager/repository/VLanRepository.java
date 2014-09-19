package ch.bergturbenthal.wisp.manager.repository;

import org.springframework.data.repository.CrudRepository;

import ch.bergturbenthal.wisp.manager.model.VLan;

public interface VLanRepository extends CrudRepository<VLan, Long> {
	// @Query("from VLan v where v.address.v4Address=?1 or v.address.v6Address=?1")
	// List<VLan> findVlanByRange(final IpRange range);
}
