package com.enviro.assessment.junior.kizitokayizzi.config;

import com.enviro.assessment.junior.kizitokayizzi.entity.AppUser;
import com.enviro.assessment.junior.kizitokayizzi.entity.Investor;
import com.enviro.assessment.junior.kizitokayizzi.entity.Product;
import com.enviro.assessment.junior.kizitokayizzi.entity.ProductType;
import com.enviro.assessment.junior.kizitokayizzi.entity.WithdrawalNotice;
import com.enviro.assessment.junior.kizitokayizzi.repository.AppUserRepository;
import com.enviro.assessment.junior.kizitokayizzi.repository.InvestorRepository;
import com.enviro.assessment.junior.kizitokayizzi.repository.ProductRepository;
import com.enviro.assessment.junior.kizitokayizzi.repository.WithdrawalNoticeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Demo data for the in-memory H2 database. The password is a demo value, documented in the README. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "Password123!";

    private final InvestorRepository investorRepository;
    private final ProductRepository productRepository;
    private final WithdrawalNoticeRepository noticeRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DataSeeder(InvestorRepository investorRepository, ProductRepository productRepository,
                      WithdrawalNoticeRepository noticeRepository, AppUserRepository userRepository,
                      PasswordEncoder passwordEncoder, Clock clock) {
        this.investorRepository = investorRepository;
        this.productRepository = productRepository;
        this.noticeRepository = noticeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** One historical withdrawal to seed, oldest first: how many days ago it happened, and the amount. */
    private record SeedWithdrawal(int daysAgo, String amount) {
    }

    @Override
    public void run(String... args) {
        // Over 65: may withdraw from retirement products. Opening balances are reduced by the history below,
        // ending at the same balances the UI previously showed so the rest of the demo stays consistent.
        seed("thabo", new Investor("Thabo", "Mokoena", LocalDate.of(1955, 4, 12), "thabo.mokoena@example.com"),
                "1000000.00", List.of(
                        new SeedWithdrawal(90, "60000.00"),
                        new SeedWithdrawal(45, "40000.00"),
                        new SeedWithdrawal(10, "50000.00")),
                "150000.00", List.of(
                        new SeedWithdrawal(120, "20000.00"),
                        new SeedWithdrawal(20, "10000.00")));

        // Under 65: retirement withdrawals must be rejected, so no retirement history is seeded
        seed("lerato", new Investor("Lerato", "Dlamini", LocalDate.of(1990, 8, 23), "lerato.dlamini@example.com"),
                "150000.00", List.of(),
                "50000.00", List.of(new SeedWithdrawal(15, "4999.50")));

        // Exactly 65 today: sits on the "age > 65" boundary, so retirement withdrawals are still rejected
        seed("sipho", new Investor("Sipho", "Nkosi", LocalDate.now(clock).minusYears(65), "sipho.nkosi@example.com"),
                "500000.00", List.of(),
                "70000.00", List.of(new SeedWithdrawal(7, "10000.00")));
    }

    private void seed(String username, Investor investor,
                      String retirementOpening, List<SeedWithdrawal> retirementHistory,
                      String savingsOpening, List<SeedWithdrawal> savingsHistory) {
        investorRepository.save(investor);

        Product retirement = productRepository.save(new Product(investor, ProductType.RETIREMENT,
                "Retirement Annuity", new BigDecimal(retirementOpening)));
        Product savings = productRepository.save(new Product(investor, ProductType.SAVINGS,
                "Tax-Free Savings", new BigDecimal(savingsOpening)));

        applyHistory(retirement, retirementHistory);
        applyHistory(savings, savingsHistory);

        userRepository.save(new AppUser(username, passwordEncoder.encode(DEMO_PASSWORD), investor));
    }

    /** Replays historical withdrawals against a product's opening balance and saves the resulting notices. */
    private void applyHistory(Product product, List<SeedWithdrawal> history) {
        BigDecimal balance = product.getBalance();
        for (SeedWithdrawal withdrawal : history) {
            BigDecimal amount = new BigDecimal(withdrawal.amount());
            BigDecimal balanceAfter = balance.subtract(amount);
            LocalDateTime createdAt = LocalDateTime.now(clock).minusDays(withdrawal.daysAgo());

            noticeRepository.save(new WithdrawalNotice(newReference(), product, amount, balance, balanceAfter,
                    createdAt));
            balance = balanceAfter;
        }
        product.setBalance(balance);
        productRepository.save(product);
    }

    private String newReference() {
        return "WN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
