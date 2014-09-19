package ch.bergturbenthal.wisp.manager.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import ch.bergturbenthal.wisp.manager.model.NetworkDevice;

public interface NetworkDeviceRepository extends CrudRepository<NetworkDevice, Long> {

	List<NetworkDevice> findBySerialNumber(final String serialNumber);

}
