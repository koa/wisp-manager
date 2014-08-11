package ch.bergturbenthal.wisp.manager.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

import lombok.Data;
import ch.bergturbenthal.wisp.manager.model.devices.NetworkDeviceType;

@Data
@Entity
public class Password {
	@Id
	@Enumerated(EnumType.STRING)
	@Column(updatable = false, nullable = false)
	private NetworkDeviceType deviceType;
	private String password;
}
