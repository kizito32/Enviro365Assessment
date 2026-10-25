package com.enviro.assessment.junior.kizitokayizzi.dto;

import java.time.LocalDate;

public record InvestorDto(Long id, String firstName, String lastName, LocalDate dateOfBirth, int age, String email) {
}
