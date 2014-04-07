package ch.bergturbenthal.wisp.manager.service.impl;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.service.StationEntityProvider;

import com.vaadin.addon.jpacontainer.provider.MutableLocalEntityProvider;

@Component
@Transactional
public class StationProviderBean extends MutableLocalEntityProvider<Station> implements StationEntityProvider {
	@PersistenceContext
	private EntityManager em;

	public StationProviderBean() {
		super(Station.class);
		setTransactionsHandledByProvider(false);
	}

	@PostConstruct
	public void init() {
		setEntityManager(em);
		/*
		 * The entity manager is transaction-scoped, which means that the entities will be automatically detached when the transaction is closed.
		 * Therefore, we do not need to explicitly detach them.
		 */
		setEntitiesDetached(false);
	}

}
