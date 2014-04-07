package ch.bergturbenthal.wisp.manager.repository;

import org.springframework.data.repository.CrudRepository;

import ch.bergturbenthal.wisp.manager.model.GlobalDnsServer;
import ch.bergturbenthal.wisp.manager.model.IpAddress;

public interface DnsServerRepository extends CrudRepository<GlobalDnsServer, IpAddress> {

}
