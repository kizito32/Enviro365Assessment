package com.enviro.assessment.junior.kizitokayizzi.exception;

/** The request itself is unusable, e.g. a date range where "from" is after "to". Mapped to HTTP 400. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
