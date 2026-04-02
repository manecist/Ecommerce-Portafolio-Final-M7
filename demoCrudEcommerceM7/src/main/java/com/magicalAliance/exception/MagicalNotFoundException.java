package com.magicalAliance.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND) // Esto le dice a Spring que es un 404 real
public class MagicalNotFoundException extends MagicalException {
    public MagicalNotFoundException(String message) {
        super(message);
    }
}