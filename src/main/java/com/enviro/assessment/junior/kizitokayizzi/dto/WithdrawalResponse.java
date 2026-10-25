package com.enviro.assessment.junior.kizitokayizzi.dto;

import com.enviro.assessment.junior.kizitokayizzi.entity.ProductType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WithdrawalResponse(Long id, String reference, Long productId, String productName,
                                 ProductType productType, BigDecimal amount, BigDecimal balanceBefore,
                                 BigDecimal balanceAfter, LocalDateTime createdAt) {
}
