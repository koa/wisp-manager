package ch.bergturbenthal.wisp.manager.repository;

import org.springframework.data.repository.CrudRepository;

import ch.bergturbenthal.wisp.manager.model.Connection;

public interface ConnectionRepository extends CrudRepository<Connection, Long> {
	// @Query(" from Connection c where addresses.v4Address=?1 or addresses.v6Address=?1")
	// Connection findConnectionForRange(final IpRange connectionRange);
}
