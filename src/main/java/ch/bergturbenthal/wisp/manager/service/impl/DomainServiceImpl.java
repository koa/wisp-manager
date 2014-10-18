package ch.bergturbenthal.wisp.manager.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.bergturbenthal.wisp.manager.model.Domain;
import ch.bergturbenthal.wisp.manager.repository.DomainRepository;
import ch.bergturbenthal.wisp.manager.service.DomainService;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;

@Component
public class DomainServiceImpl implements DomainService {

	@Autowired
	private DomainRepository domainRepository;

	@Override
	public CrudRepositoryContainer<Domain, Long> createContainerRepository() {
		return new CrudRepositoryContainer<Domain, Long>(domainRepository, Domain.class) {
			@Override
			protected Long idFromValue(final Domain entry) {
				return entry.getId();
			}
		};
	}

}
