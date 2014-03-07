package ch.bergturbenthal.wisp.manager.service;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaQuery;

import ch.bergturbenthal.wisp.manager.model.Position;
import ch.bergturbenthal.wisp.manager.model.Station;

@Stateless
public class StationServiceImpl implements StationService {
	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public Station addStation(final Position position) {
		final Station station = new Station();
		station.setPosition(position);
		entityManager.persist(station);
		station.setName("Station-" + station.getId());
		entityManager.persist(station);
		return station;
	}

	@Override
	public Iterable<Station> listAllStations() {
		final CriteriaQuery<Station> criteriaQuery = entityManager.getCriteriaBuilder().createQuery(Station.class);
		criteriaQuery.from(Station.class);
		return entityManager.createQuery(criteriaQuery).getResultList();
	}

	@Override
	public Station moveStation(final long station, final Position newPosition) {
		final Station storedStation = entityManager.find(Station.class, Long.valueOf(station));
		if (storedStation == null) {
			return null;
		}
		storedStation.setPosition(newPosition);
		entityManager.persist(storedStation);
		return storedStation;
	}

}
