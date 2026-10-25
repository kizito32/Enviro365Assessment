package com.enviro.assessment.junior.kizitokayizzi.service;

import com.enviro.assessment.junior.kizitokayizzi.dto.WithdrawalRequest;
import com.enviro.assessment.junior.kizitokayizzi.dto.WithdrawalResponse;
import com.enviro.assessment.junior.kizitokayizzi.entity.Product;
import com.enviro.assessment.junior.kizitokayizzi.entity.WithdrawalNotice;
import com.enviro.assessment.junior.kizitokayizzi.exception.InvalidRequestException;
import com.enviro.assessment.junior.kizitokayizzi.exception.ResourceNotFoundException;
import com.enviro.assessment.junior.kizitokayizzi.repository.ProductRepository;
import com.enviro.assessment.junior.kizitokayizzi.repository.WithdrawalNoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WithdrawalService {

    private static final LocalDate EARLIEST_DATE = LocalDate.of(1970, 1, 1);

    private final ProductRepository productRepository;
    private final WithdrawalNoticeRepository noticeRepository;
    private final WithdrawalPolicy policy;
    private final Clock clock;

    public WithdrawalService(ProductRepository productRepository, WithdrawalNoticeRepository noticeRepository,
                             WithdrawalPolicy policy, Clock clock) {
        this.productRepository = productRepository;
        this.noticeRepository = noticeRepository;
        this.policy = policy;
        this.clock = clock;
    }

    /** Validates the rules, deducts the balance and stores the notice in one transaction. */
    @Transactional
    public WithdrawalResponse createNotice(Long investorId, WithdrawalRequest request) {
        // Looking the product up by investor id means a user cannot touch someone else's product
        Product product = productRepository.findOwnedForUpdate(request.productId(), investorId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        BigDecimal amount = request.amount().setScale(2);
        policy.validate(product.getInvestor(), product, amount);

        BigDecimal balanceBefore = product.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(amount);
        product.setBalance(balanceAfter);

        WithdrawalNotice notice = new WithdrawalNotice(newReference(), product, amount,
                balanceBefore, balanceAfter, LocalDateTime.now(clock));
        return toResponse(noticeRepository.save(notice));
    }

    @Transactional(readOnly = true)
    public List<WithdrawalResponse> history(Long investorId, LocalDate from, LocalDate to, Long productId) {
        LocalDate effectiveFrom = from != null ? from : EARLIEST_DATE;
        LocalDate effectiveTo = to != null ? to : LocalDate.now(clock);
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new InvalidRequestException("'from' date must not be after 'to' date");
        }

        // "to" is a whole day, so the query uses the start of the next day as an exclusive bound
        return noticeRepository.search(investorId, productId,
                        effectiveFrom.atStartOfDay(), effectiveTo.plusDays(1).atStartOfDay())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public String exportCsv(Long investorId, LocalDate from, LocalDate to, Long productId) {
        return CsvStatementWriter.write(history(investorId, from, to, productId));
    }

    private String newReference() {
        return "WN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private WithdrawalResponse toResponse(WithdrawalNotice notice) {
        Product product = notice.getProduct();
        return new WithdrawalResponse(notice.getId(), notice.getReference(), product.getId(), product.getName(),
                product.getType(), notice.getAmount(), notice.getBalanceBefore(), notice.getBalanceAfter(),
                notice.getCreatedAt());
    }
}
