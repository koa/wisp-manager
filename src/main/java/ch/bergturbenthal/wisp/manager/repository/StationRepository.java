package ch.bergturbenthal.wisp.manager.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import ch.bergturbenthal.wisp.manager.model.Station;

public interface StationRepository extends CrudRepository<Station, Long> {
	@Query("select s from Station s where tunnelConnection=true")
	Collection<Station> findTunnelConnectionStations();
}
