package com.enviro.assessment.junior.kizitokayizzi.config;

import com.enviro.assessment.junior.kizitokayizzi.entity.AppUser;
import com.enviro.assessment.junior.kizitokayizzi.entity.Investor;
import com.enviro.assessment.junior.kizitokayizzi.entity.Product;
import com.enviro.assessment.junior.kizitokayizzi.entity.ProductType;
import com.enviro.assessment.junior.kizitokayizzi.repository.AppUserRepository;
import com.enviro.assessment.junior.kizitokayizzi.repository.InvestorRepository;
import com.enviro.assessment.junior.kizitokayizzi.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

/** Demo data for the in-memory H2 database. The password is a demo value, documented in the README. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "Password123!";

    private final InvestorRepository investorRepository;
    private final ProductRepository productRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DataSeeder(InvestorRepository investorRepository, ProductRepository productRepository,
                      AppUserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.investorRepository = investorRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        // Over 65: may withdraw from retirement products
        seed("thabo", new Investor("Thabo", "Mokoena", LocalDate.of(1955, 4, 12), "thabo.mokoena@example.com"),
                "850000.00", "120000.00");

        // Under 65: retirement withdrawals must be rejected
        seed("lerato", new Investor("Lerato", "Dlamini", LocalDate.of(1990, 8, 23), "lerato.dlamini@example.com"),
                "150000.00", "45000.50");

        // Exactly 65 today: sits on the "age > 65" boundary, so retirement withdrawals are still rejected
        seed("sipho", new Investor("Sipho", "Nkosi", LocalDate.now(clock).minusYears(65), "sipho.nkosi@example.com"),
                "500000.00", "60000.00");
    }

    private void seed(String username, Investor investor, String retirementBalance, String savingsBalance) {
        investorRepository.save(investor);
        productRepository.save(new Product(investor, ProductType.RETIREMENT, "Retirement Annuity",
                new BigDecimal(retirementBalance)));
        productRepository.save(new Product(investor, ProductType.SAVINGS, "Tax-Free Savings",
                new BigDecimal(savingsBalance)));
        userRepository.save(new AppUser(username, passwordEncoder.encode(DEMO_PASSWORD), investor));
    }
}
