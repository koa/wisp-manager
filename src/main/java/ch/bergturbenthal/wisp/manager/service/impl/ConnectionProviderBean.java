package ch.bergturbenthal.wisp.manager.service.impl;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.bergturbenthal.wisp.manager.model.Connection;
import ch.bergturbenthal.wisp.manager.service.AddressManagementService;
import ch.bergturbenthal.wisp.manager.service.ConnectionEntityProvider;

import com.vaadin.addon.jpacontainer.provider.MutableLocalEntityProvider;

@Component
@Transactional
public class ConnectionProviderBean extends MutableLocalEntityProvider<Connection> implements ConnectionEntityProvider {

	@Autowired
	private AddressManagementService addressManagementBean;
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
