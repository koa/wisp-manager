package ch.bergturbenthal.wisp.manager.service;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import ch.bergturbenthal.wisp.manager.model.Connection;

import com.vaadin.addon.jpacontainer.provider.MutableLocalEntityProvider;

@Stateless
@TransactionManagement
public class ConnectionProviderBean extends MutableLocalEntityProvider<Connection> {
	@EJB
	private AddressManagementBean addressManagementBean;
	@PersistenceContext
	private EntityManager em;

	public ConnectionProviderBean() {
		super(Connection.class);
		setTransactionsHandledByProvider(false);
	}

	@Override
	public Connection addEntity(final Connection entity) {
		final Connection newEntity = super.addEntity(entity);
		addressManagementBean.fillConnection(newEntity);
		return newEntity;
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
