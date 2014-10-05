package ch.bergturbenthal.wisp.manager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import ch.bergturbenthal.wisp.manager.model.PortExpose;

public interface PortExposeRepository extends CrudRepository<PortExpose, Long> {
	@Query("select p from PortExpose p where p.portNumber=?1 and p.targetAddress.addressType=ch.bergturbenthal.wisp.manager.model.address.IpAddressType.V4")
	List<PortExpose> findV4ByPortNumber(final int portNumber);
}
