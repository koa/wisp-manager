package ch.bergturbenthal.wisp.manager.model.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ch.bergturbenthal.wisp.manager.model.Antenna;

public class AntennaValidator implements ConstraintValidator<ValidateAntenna, Antenna> {

	@Override
	public void initialize(final ValidateAntenna constraintAnnotation) {

	}

	@Override
	public boolean isValid(final Antenna value, final ConstraintValidatorContext context) {
		return value.getApBridge() == null ^ value.getClientBridge() == null;
	}

}
