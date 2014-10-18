package ch.bergturbenthal.wisp.manager.service;

import ch.bergturbenthal.wisp.manager.model.Domain;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;

public interface DomainService {
	CrudRepositoryContainer<Domain, Long> createContainerRepository();
}
