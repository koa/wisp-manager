package ch.bergturbenthal.wisp.manager.repository;

import org.springframework.data.repository.CrudRepository;

import ch.bergturbenthal.wisp.manager.model.GatewaySettings;

public interface GatewaySettingsRepository extends CrudRepository<GatewaySettings, Long> {
}
