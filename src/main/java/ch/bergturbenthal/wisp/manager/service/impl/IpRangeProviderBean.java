package ch.bergturbenthal.wisp.manager.service.impl;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.service.IpRangeEntityProvider;

import com.vaadin.addon.jpacontainer.provider.MutableLocalEntityProvider;

@Component
@Transactional
public class IpRangeProviderBean extends MutableLocalEntityProvider<IpRange> implements IpRangeEntityProvider {
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
