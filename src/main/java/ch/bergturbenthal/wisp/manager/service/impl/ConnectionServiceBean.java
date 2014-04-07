package ch.bergturbenthal.wisp.manager.service.impl;

import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.service.ConnectionService;
import ch.bergturbenthal.wisp.manager.service.EntityUtil;

@Component
@Transactional
public class ConnectionServiceBean implements ConnectionService {
	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public Connection connectStations(final Station s1, final Station s2) {
		final Station s1Merged = entityManager.merge(s1);
		final Station s2Merged = entityManager.merge(s2);
		final Connection connection = new Connection();
		connection.setStartStation(s1Merged);
		connection.setEndStation(s2Merged);
		s1Merged.getBeginningConnections().add(connection);
		s2Merged.getEndingConnections().add(connection);
		entityManager.persist(connection);
		return connection;
	}

	@Override
	public Collection<Connection> listAllConnections() {
		return EntityUtil.queryAll(Connection.class, entityManager);
	}

}
