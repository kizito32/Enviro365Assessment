package com.enviro.assessment.junior.kizitokayizzi.service;

import com.enviro.assessment.junior.kizitokayizzi.entity.Investor;
import com.enviro.assessment.junior.kizitokayizzi.entity.Product;
import com.enviro.assessment.junior.kizitokayizzi.entity.ProductType;
import com.enviro.assessment.junior.kizitokayizzi.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

/** The single place that holds the withdrawal business rules. */
@Component
public class WithdrawalPolicy {

    static final int RETIREMENT_MIN_AGE_EXCLUSIVE = 65;
    private static final BigDecimal MAX_WITHDRAWAL_RATIO = new BigDecimal("0.90");

    // Injected so tests can freeze "today" and check the age-65 boundary reliably
    private final Clock clock;

    public WithdrawalPolicy(Clock clock) {
        this.clock = clock;
    }

    public int ageOf(Investor investor) {
        return Period.between(investor.getDateOfBirth(), LocalDate.now(clock)).getYears();
    }

    public boolean isEligible(Investor investor, ProductType type) {
        return type != ProductType.RETIREMENT || ageOf(investor) > RETIREMENT_MIN_AGE_EXCLUSIVE;
    }

    /** 90% of the balance, rounded down so the limit can never be exceeded by rounding. */
    public BigDecimal maxWithdrawal(Product product) {
        return product.getBalance().multiply(MAX_WITHDRAWAL_RATIO).setScale(2, RoundingMode.DOWN);
    }

    public void validate(Investor investor, Product product, BigDecimal amount) {
        if (!isEligible(investor, product.getType())) {
            throw new BusinessRuleException("Retirement withdrawals are only allowed for investors older than 65.");
        }
        if (amount.compareTo(product.getBalance()) > 0) {
            throw new BusinessRuleException("Withdrawal amount exceeds the available balance.");
        }
        BigDecimal max = maxWithdrawal(product);
        if (amount.compareTo(max) > 0) {
            throw new BusinessRuleException(
                    "Withdrawal may not exceed 90% of the balance (maximum " + max.toPlainString() + ").");
        }
    }
}
