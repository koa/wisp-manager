package ch.bergturbenthal.wisp.manager.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.IpRange;

public interface ConnectionRepository extends CrudRepository<Connection, Long> {
	@Query(" from Connection c where addresses.v4Address=?1 or addresses.v6Address=?1")
	Connection findConnectionForRange(final IpRange connectionRange);
}
