package ch.bergturbenthal.wisp.manager.service;

import java.util.Collection;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import ch.bergturbenthal.wisp.manager.model.Connection;

@Stateless
public class ConnectionServiceBean implements ConnectionService {
	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public Collection<Connection> listAllConnections() {
		return EntityUtil.queryAll(Connection.class, entityManager);
	}

}
