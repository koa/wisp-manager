package ch.bergturbenthal.wisp.manager.service;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import ch.bergturbenthal.wisp.manager.model.IpReservationRange;

import com.vaadin.addon.jpacontainer.provider.MutableLocalEntityProvider;

@Stateless
@TransactionManagement
public class IpAddressReservationRangeProviderBean extends MutableLocalEntityProvider<IpReservationRange> {
	@PersistenceContext
	private EntityManager em;

	public IpAddressReservationRangeProviderBean() {
		super(IpReservationRange.class);
		setTransactionsHandledByProvider(false);
	}

	@PostConstruct
	public void init() {
		setEntityManager(em);
		setEntitiesDetached(false);
	}

}
