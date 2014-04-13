package ch.bergturbenthal.wisp.manager.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import ch.bergturbenthal.wisp.manager.model.IpRange;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;

public interface NetworkDeviceRepository extends CrudRepository<NetworkDevice, Long> {
	@Query("select nd from NetworkDevice nd inner join nd.interfaces ni inner join ni.networks v where v.address.v4Address=?1 or v.address.v6Address=?1")
	NetworkDevice findDeviceForRange(final IpRange deviceRange);

}
