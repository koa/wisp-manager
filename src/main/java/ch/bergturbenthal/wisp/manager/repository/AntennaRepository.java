package ch.bergturbenthal.wisp.manager.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import ch.bergturbenthal.wisp.manager.model.Antenna;
import ch.bergturbenthal.wisp.manager.model.IpRange;

public interface AntennaRepository extends CrudRepository<Antenna, Long> {
	@Query(" from Antenna where addresses.v4Address=?1 or addresses.v6Address=?1")
	Antenna findAntennaForRange(final IpRange connectionRange);

}
