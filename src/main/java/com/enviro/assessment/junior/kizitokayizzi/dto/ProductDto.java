package com.enviro.assessment.junior.kizitokayizzi.dto;

import com.enviro.assessment.junior.kizitokayizzi.entity.ProductType;

import java.math.BigDecimal;

/**
 * maxWithdrawal and withdrawable are hints for the UI only; the server re-checks every rule on submit.
 */
public record ProductDto(Long id, ProductType type, String name, BigDecimal balance,
                         BigDecimal maxWithdrawal, boolean withdrawable) {
}
