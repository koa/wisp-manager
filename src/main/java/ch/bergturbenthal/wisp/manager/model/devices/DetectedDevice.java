package ch.bergturbenthal.wisp.manager.model.devices;

import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.experimental.Builder;
import ch.bergturbenthal.wisp.manager.model.MacAddress;

@Data
@Builder
public class DetectedDevice {
	private final List<MacAddress> interfaces;
	private final NetworkDeviceModel model;
	private final Map<String, String> properties;
	private final String serialNumber;
}
