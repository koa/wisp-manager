package ch.bergturbenthal.wisp.manager.view.component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.model.Station;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceModel;
import ch.bergturbenthal.wisp.manager.service.DomainService;
import ch.bergturbenthal.wisp.manager.service.NetworkDeviceManagementService;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer.PojoFilter;

@Configuration
public class ContainerSelectFactory {
	private CrudRepositoryContainer<NetworkDevice, Long> devicesContainer;
	@Autowired
	private DomainService domainService;
	@Autowired
	private NetworkDeviceManagementService networkDeviceManagementService;

	@PostConstruct
	private void createDevicesContainer() {
		devicesContainer = networkDeviceManagementService.createContainerRepository();
	}

	@Bean
	public ContainerSelectFieldFactory createFieldFactory() {
		return new ContainerSelectFieldFactory(devicesContainer, domainService.createContainerRepository());
	}

	@Bean
	public CurrentSelectedDeviceHandler createSelectedStationHandler() {
		return new CurrentSelectedDeviceHandler() {

			@Override
			public void setFilterForStation(final Station station) {
				final Set<NetworkDeviceModel> stationModels = new HashSet<NetworkDeviceModel>(Arrays.asList(NetworkDeviceModel.stationModels));
				devicesContainer.removeAllFilters();
				final Long stationId = station.getId();
				devicesContainer.addFilter(new PojoFilter<NetworkDevice>() {
					@Override
					public boolean accept(final NetworkDevice candidate) {
						if (!stationModels.contains(candidate.getDeviceModel())) {
							return false;
						}
						return candidate.getStation() == null || candidate.getStation().getId().equals(stationId);
					}
				});

			}
		};
	}

}
