package ch.bergturbenthal.wisp.manager.service;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import ch.bergturbenthal.wisp.manager.model.IpRange;

import com.vaadin.addon.jpacontainer.provider.MutableLocalEntityProvider;

@Stateless
@TransactionManagement
public class IpRangeProviderBean extends MutableLocalEntityProvider<IpRange> {
	@PersistenceContext
	private EntityManager em;

	public IpRangeProviderBean() {
		super(IpRange.class);
		setTransactionsHandledByProvider(false);
	}

	@PostConstruct
	public void init() {
		setEntityManager(em);
		setEntitiesDetached(false);
	}

}
