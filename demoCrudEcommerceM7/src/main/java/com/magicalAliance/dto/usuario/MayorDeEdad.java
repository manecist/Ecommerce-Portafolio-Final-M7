package com.magicalAliance.dto.usuario;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MayorDeEdadValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MayorDeEdad {
    String message() default "Debes ser mayor de 18 años para registrarte";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}