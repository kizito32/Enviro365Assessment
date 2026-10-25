package com.enviro.assessment.junior.kizitokayizzi.controller;

import com.enviro.assessment.junior.kizitokayizzi.dto.PortfolioResponse;
import com.enviro.assessment.junior.kizitokayizzi.security.InvestorUserDetails;
import com.enviro.assessment.junior.kizitokayizzi.service.PortfolioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    // The investor comes from the login, not from the URL, so nobody can request another investor's portfolio
    @GetMapping
    public PortfolioResponse getPortfolio(@AuthenticationPrincipal InvestorUserDetails user) {
        return portfolioService.getPortfolio(user.getInvestorId());
    }
}
