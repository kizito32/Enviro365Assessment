package com.enviro.assessment.junior.kizitokayizzi.service;

import com.enviro.assessment.junior.kizitokayizzi.dto.InvestorDto;
import com.enviro.assessment.junior.kizitokayizzi.dto.PortfolioResponse;
import com.enviro.assessment.junior.kizitokayizzi.dto.ProductDto;
import com.enviro.assessment.junior.kizitokayizzi.entity.Investor;
import com.enviro.assessment.junior.kizitokayizzi.entity.Product;
import com.enviro.assessment.junior.kizitokayizzi.exception.ResourceNotFoundException;
import com.enviro.assessment.junior.kizitokayizzi.repository.InvestorRepository;
import com.enviro.assessment.junior.kizitokayizzi.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PortfolioService {

    private final InvestorRepository investorRepository;
    private final ProductRepository productRepository;
    private final WithdrawalPolicy policy;

    public PortfolioService(InvestorRepository investorRepository, ProductRepository productRepository,
                            WithdrawalPolicy policy) {
        this.investorRepository = investorRepository;
        this.productRepository = productRepository;
        this.policy = policy;
    }

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(Long investorId) {
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new ResourceNotFoundException("Investor not found"));

        List<ProductDto> products = productRepository.findByInvestorIdOrderById(investorId).stream()
                .map(product -> toDto(investor, product))
                .toList();

        BigDecimal total = products.stream()
                .map(ProductDto::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        InvestorDto investorDto = new InvestorDto(investor.getId(), investor.getFirstName(),
                investor.getLastName(), investor.getDateOfBirth(), policy.ageOf(investor), investor.getEmail());

        return new PortfolioResponse(investorDto, products, total);
    }

    private ProductDto toDto(Investor investor, Product product) {
        boolean withdrawable = policy.isEligible(investor, product.getType());
        BigDecimal max = withdrawable ? policy.maxWithdrawal(product) : BigDecimal.ZERO.setScale(2);
        return new ProductDto(product.getId(), product.getType(), product.getName(),
                product.getBalance(), max, withdrawable);
    }
}
