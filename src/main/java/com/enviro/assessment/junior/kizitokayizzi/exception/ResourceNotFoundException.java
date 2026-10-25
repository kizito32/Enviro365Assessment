package com.enviro.assessment.junior.kizitokayizzi.exception;

/** The requested record does not exist, or does not belong to the caller. Mapped to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
