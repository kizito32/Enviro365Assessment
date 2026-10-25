package com.enviro.assessment.junior.kizitokayizzi.service;

import com.enviro.assessment.junior.kizitokayizzi.dto.WithdrawalRequest;
import com.enviro.assessment.junior.kizitokayizzi.dto.WithdrawalResponse;
import com.enviro.assessment.junior.kizitokayizzi.entity.Investor;
import com.enviro.assessment.junior.kizitokayizzi.entity.Product;
import com.enviro.assessment.junior.kizitokayizzi.entity.ProductType;
import com.enviro.assessment.junior.kizitokayizzi.entity.WithdrawalNotice;
import com.enviro.assessment.junior.kizitokayizzi.exception.BusinessRuleException;
import com.enviro.assessment.junior.kizitokayizzi.exception.InvalidRequestException;
import com.enviro.assessment.junior.kizitokayizzi.exception.ResourceNotFoundException;
import com.enviro.assessment.junior.kizitokayizzi.repository.ProductRepository;
import com.enviro.assessment.junior.kizitokayizzi.repository.WithdrawalNoticeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    private static final Long INVESTOR_ID = 7L;
    private static final Long PRODUCT_ID = 3L;

    @Mock
    private ProductRepository productRepository;
    @Mock
    private WithdrawalNoticeRepository noticeRepository;

    private WithdrawalService service;
    private Product savings;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-25T10:00:00Z"), ZoneOffset.UTC);
        service = new WithdrawalService(productRepository, noticeRepository, new WithdrawalPolicy(clock), clock);

        Investor investor = new Investor("Lerato", "Dlamini", LocalDate.of(1990, 8, 23), "lerato@example.com");
        savings = new Product(investor, ProductType.SAVINGS, "Tax-Free Savings", new BigDecimal("1000.00"));
    }

    @Test
    void createNoticeDeductsBalanceAndStoresNotice() {
        when(productRepository.findOwnedForUpdate(PRODUCT_ID, INVESTOR_ID)).thenReturn(Optional.of(savings));
        when(noticeRepository.save(any(WithdrawalNotice.class))).thenAnswer(call -> call.getArgument(0));

        WithdrawalResponse response = service.createNotice(INVESTOR_ID,
                new WithdrawalRequest(PRODUCT_ID, new BigDecimal("250")));

        assertThat(savings.getBalance()).isEqualByComparingTo("750.00");
        assertThat(response.amount()).isEqualByComparingTo("250.00");
        assertThat(response.balanceBefore()).isEqualByComparingTo("1000.00");
        assertThat(response.balanceAfter()).isEqualByComparingTo("750.00");
        assertThat(response.reference()).startsWith("WN-");

        ArgumentCaptor<WithdrawalNotice> saved = ArgumentCaptor.forClass(WithdrawalNotice.class);
        verify(noticeRepository).save(saved.capture());
        assertThat(saved.getValue().getCreatedAt()).hasToString("2026-10-25T10:00");
    }

    @Test
    void createNoticeFailsWhenProductDoesNotBelongToInvestor() {
        when(productRepository.findOwnedForUpdate(PRODUCT_ID, INVESTOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createNotice(INVESTOR_ID,
                new WithdrawalRequest(PRODUCT_ID, new BigDecimal("10.00"))))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(noticeRepository, never()).save(any());
    }

    @Test
    void ruleViolationLeavesBalanceUntouchedAndStoresNothing() {
        when(productRepository.findOwnedForUpdate(PRODUCT_ID, INVESTOR_ID)).thenReturn(Optional.of(savings));

        assertThatThrownBy(() -> service.createNotice(INVESTOR_ID,
                new WithdrawalRequest(PRODUCT_ID, new BigDecimal("950.00"))))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(savings.getBalance()).isEqualByComparingTo("1000.00");
        verify(noticeRepository, never()).save(any());
    }

    @Test
    void historyRejectsFromDateAfterToDate() {
        assertThatThrownBy(() -> service.history(INVESTOR_ID, LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 10, 1), null))
                .isInstanceOf(InvalidRequestException.class);
    }
}
