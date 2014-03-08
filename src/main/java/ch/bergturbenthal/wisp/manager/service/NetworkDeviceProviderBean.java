package ch.bergturbenthal.wisp.manager.service;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import ch.bergturbenthal.wisp.manager.model.NetworkDevice;

import com.vaadin.addon.jpacontainer.provider.MutableLocalEntityProvider;

@Stateless
@TransactionManagement
public class NetworkDeviceProviderBean extends MutableLocalEntityProvider<NetworkDevice> {
	@PersistenceContext
	private EntityManager em;

	public NetworkDeviceProviderBean() {
		super(NetworkDevice.class);
		setTransactionsHandledByProvider(false);
	}

	@PostConstruct
	public void init() {
		setEntityManager(em);
		setEntitiesDetached(false);
	}

}
