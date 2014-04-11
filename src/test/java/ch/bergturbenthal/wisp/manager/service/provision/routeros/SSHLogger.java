package ch.bergturbenthal.wisp.manager.service.provision.routeros;

import lombok.extern.slf4j.Slf4j;

import com.jcraft.jsch.Logger;

@Slf4j
public final class SSHLogger implements Logger {
	@Override
	public boolean isEnabled(final int level) {
		return false;
	}

	@Override
	public void log(final int level, final String message) {
		switch (level) {
		case DEBUG:
			log.debug(message);
			break;
		case INFO:
			log.info(message);
			break;
		case WARN:
			log.warn(message);
			break;
		case ERROR:
			log.error(message);
			break;
		case FATAL:
			log.error(message);
			break;
		}
	}
}