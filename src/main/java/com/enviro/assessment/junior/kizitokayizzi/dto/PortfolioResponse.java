package com.enviro.assessment.junior.kizitokayizzi.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(InvestorDto investor, List<ProductDto> products, BigDecimal totalBalance) {
}
