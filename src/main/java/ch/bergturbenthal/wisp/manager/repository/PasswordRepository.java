package ch.bergturbenthal.wisp.manager.repository;

import org.springframework.data.repository.CrudRepository;

import ch.bergturbenthal.wisp.manager.model.Password;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceType;

public interface PasswordRepository extends CrudRepository<Password, NetworkDeviceType> {

}
