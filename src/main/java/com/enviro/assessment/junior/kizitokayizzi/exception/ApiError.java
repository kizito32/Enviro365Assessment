package com.enviro.assessment.junior.kizitokayizzi.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/** One consistent error shape for every failure the API returns. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(Instant timestamp, int status, String error, String message,
                       Map<String, String> fieldErrors) {
}
