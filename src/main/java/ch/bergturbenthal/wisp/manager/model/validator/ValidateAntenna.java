package ch.bergturbenthal.wisp.manager.model.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Constraint(validatedBy = AntennaValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateAntenna {
	Class<?>[] groups() default {};

	String message() default "invalid antenna definition";

	Class<? extends Payload>[] payload() default {};
}
