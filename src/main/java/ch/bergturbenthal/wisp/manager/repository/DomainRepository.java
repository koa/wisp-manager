package ch.bergturbenthal.wisp.manager.repository;

import org.springframework.data.repository.CrudRepository;

import ch.bergturbenthal.wisp.manager.model.Domain;

public interface DomainRepository extends CrudRepository<Domain, Long> {
	public Domain findByDomainName(final String domainName);
}
