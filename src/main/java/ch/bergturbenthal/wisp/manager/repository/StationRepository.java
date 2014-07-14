package ch.bergturbenthal.wisp.manager.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import ch.bergturbenthal.wisp.manager.model.CustomerConnection;
import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.model.Station;

public interface StationRepository extends CrudRepository<Station, Long> {
	@Query("select s from Station s where loopback.v4Address=?1 or loopback.v6Address=?1")
	Station findStationLoopbackForRange(final IpRange range);

	@Query("select s from CustomerConnection s inner join s.ownNetworks n where n.address.v4Address=?1 or n.address.v6Address=?1")
	CustomerConnection findStationNetworkForRange(final IpRange range);
}
