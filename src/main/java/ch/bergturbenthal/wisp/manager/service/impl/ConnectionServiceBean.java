package ch.bergturbenthal.wisp.manager.service.impl;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.repository.ConnectionRepository;
import ch.bergturbenthal.wisp.manager.service.ConnectionService;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;

@Component
@Transactional
public class ConnectionServiceBean implements ConnectionService {
	@Autowired
	private ConnectionRepository connectionRepository;
	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public Connection connectStations(final Station s1, final Station s2) {
		final Connection connection = new Connection();
		connection.setStartStation(s1);
		connection.setEndStation(s2);
		// s1.getBeginningConnections().add(connection);
		// s2.getEndingConnections().add(connection);
		return connectionRepository.save(connection);
	}

	@Override
	public Iterable<Connection> listAllConnections() {
		return connectionRepository.findAll();
	}

	@Override
	public CrudRepositoryContainer<Connection, Long> makeContainer() {
		return new CrudRepositoryContainer<Connection, Long>(connectionRepository, Connection.class) {

			@Override
			protected Long idFromValue(final Connection entry) {
				return entry.getId();
			}
		};
	}

}
