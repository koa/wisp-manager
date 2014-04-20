package ch.bergturbenthal.wisp.manager.service.impl;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.stereotype.Component;

import ch.bergturbenthal.wisp.manager.service.PasswordGenerator;

@Component
public class PasswordGeneratorImpl implements PasswordGenerator {

	@Override
	public String generatePassword(final int length) {
		return RandomStringUtils.randomAlphanumeric(length);
	}

}
