package com.enviro.assessment.junior.kizitokayizzi.exception;

/** The request is well-formed but breaks a business rule (age, balance, 90% limit). Mapped to HTTP 422. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
