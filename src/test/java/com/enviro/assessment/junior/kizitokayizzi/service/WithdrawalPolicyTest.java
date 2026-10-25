package com.enviro.assessment.junior.kizitokayizzi.service;

import com.enviro.assessment.junior.kizitokayizzi.entity.Investor;
import com.enviro.assessment.junior.kizitokayizzi.entity.Product;
import com.enviro.assessment.junior.kizitokayizzi.entity.ProductType;
import com.enviro.assessment.junior.kizitokayizzi.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WithdrawalPolicyTest {

    // "Today" is frozen so the age boundary tests never break as time passes
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-25T10:00:00Z"), ZoneOffset.UTC);
    private final WithdrawalPolicy policy = new WithdrawalPolicy(clock);

    private Investor investorBorn(LocalDate dateOfBirth) {
        return new Investor("Test", "Investor", dateOfBirth, "test@example.com");
    }

    private Product product(Investor investor, ProductType type, String balance) {
        return new Product(investor, type, "Test product", new BigDecimal(balance));
    }

    @Test
    void retirementWithdrawalIsRejectedAtExactlyAge65() {
        Investor investor = investorBorn(LocalDate.of(1961, 10, 25)); // turns 65 today
        Product retirement = product(investor, ProductType.RETIREMENT, "1000.00");

        assertThat(policy.ageOf(investor)).isEqualTo(65);
        assertThatThrownBy(() -> policy.validate(investor, retirement, new BigDecimal("100.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("older than 65");
    }

    @Test
    void retirementWithdrawalIsAllowedAtAge66() {
        Investor investor = investorBorn(LocalDate.of(1960, 10, 25));
        Product retirement = product(investor, ProductType.RETIREMENT, "1000.00");

        assertThatCode(() -> policy.validate(investor, retirement, new BigDecimal("100.00")))
                .doesNotThrowAnyException();
    }

    @Test
    void savingsWithdrawalHasNoAgeRestriction() {
        Investor investor = investorBorn(LocalDate.of(2000, 1, 1));
        Product savings = product(investor, ProductType.SAVINGS, "1000.00");

        assertThatCode(() -> policy.validate(investor, savings, new BigDecimal("100.00")))
                .doesNotThrowAnyException();
    }

    @Test
    void withdrawalAboveBalanceIsRejected() {
        Investor investor = investorBorn(LocalDate.of(1990, 1, 1));
        Product savings = product(investor, ProductType.SAVINGS, "1000.00");

        assertThatThrownBy(() -> policy.validate(investor, savings, new BigDecimal("1000.01")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exceeds the available balance");
    }

    @Test
    void withdrawalAbove90PercentIsRejectedButExactly90PercentIsAllowed() {
        Investor investor = investorBorn(LocalDate.of(1990, 1, 1));
        Product savings = product(investor, ProductType.SAVINGS, "1000.00");

        assertThatCode(() -> policy.validate(investor, savings, new BigDecimal("900.00")))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.validate(investor, savings, new BigDecimal("900.01")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("90%");
    }

    @Test
    void maximumWithdrawalIsRoundedDownSoTheLimitIsNeverExceeded() {
        Investor investor = investorBorn(LocalDate.of(1990, 1, 1));
        Product savings = product(investor, ProductType.SAVINGS, "100.05"); // 90% = 90.045

        assertThat(policy.maxWithdrawal(savings)).isEqualByComparingTo("90.04");
        assertThatThrownBy(() -> policy.validate(investor, savings, new BigDecimal("90.05")))
                .isInstanceOf(BusinessRuleException.class);
    }
}
