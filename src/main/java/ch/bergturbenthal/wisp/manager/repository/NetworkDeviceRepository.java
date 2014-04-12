package ch.bergturbenthal.wisp.manager.repository;

import org.springframework.data.repository.CrudRepository;

import ch.bergturbenthal.wisp.manager.model.NetworkDevice;

public interface NetworkDeviceRepository extends CrudRepository<NetworkDevice, Long> {
}
