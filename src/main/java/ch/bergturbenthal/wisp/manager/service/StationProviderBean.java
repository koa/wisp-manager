package ch.bergturbenthal.wisp.manager.service;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import ch.bergturbenthal.wisp.manager.model.Station;

import com.vaadin.addon.jpacontainer.provider.MutableLocalEntityProvider;

@Stateless
@TransactionManagement
public class StationProviderBean extends MutableLocalEntityProvider<Station> {
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
