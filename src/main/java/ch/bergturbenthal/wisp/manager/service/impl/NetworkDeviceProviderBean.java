package ch.bergturbenthal.wisp.manager.service.impl;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.service.NetworkDeviceEntityProvider;

import com.vaadin.addon.jpacontainer.provider.MutableLocalEntityProvider;

@Component
@Transactional
public class NetworkDeviceProviderBean extends MutableLocalEntityProvider<NetworkDevice> implements NetworkDeviceEntityProvider {
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
