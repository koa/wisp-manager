package ch.bergturbenthal.wisp.manager.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.model.Station;

public interface StationRepository extends CrudRepository<Station, Long> {
	@Query("select s from Station s where loopback.v4Address=?1 or loopback.v6Address=?1")
	Station findStationForRange(final IpRange range);
}