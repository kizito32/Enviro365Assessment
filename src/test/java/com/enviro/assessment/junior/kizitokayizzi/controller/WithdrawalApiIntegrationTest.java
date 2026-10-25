package com.enviro.assessment.junior.kizitokayizzi.controller;

import com.enviro.assessment.junior.kizitokayizzi.entity.Product;
import com.enviro.assessment.junior.kizitokayizzi.entity.ProductType;
import com.enviro.assessment.junior.kizitokayizzi.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end API tests through the real security filter, validation, exception handler and H2 database. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // each test rolls back, so withdrawals do not leak between tests
class WithdrawalApiIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ProductRepository productRepository;

    private Long productId(String investorEmail, ProductType type) {
        List<Product> all = productRepository.findAll();
        return all.stream()
                .filter(p -> p.getInvestor().getEmail().equals(investorEmail) && p.getType() == type)
                .findFirst().orElseThrow().getId();
    }

    private String body(Long productId, String amount) {
        return "{\"productId\":" + productId + ",\"amount\":" + amount + "}";
    }

    @Test
    void apiRejectsRequestsWithoutCredentials() throws Exception {
        mockMvc.perform(get("/api/portfolio"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void apiRejectsWrongPassword() throws Exception {
        mockMvc.perform(get("/api/portfolio").with(httpBasic("lerato", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void portfolioReturnsOnlyTheLoggedInInvestorsData() throws Exception {
        mockMvc.perform(get("/api/portfolio").with(httpBasic("lerato", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.investor.firstName").value("Lerato"))
                .andExpect(jsonPath("$.products.length()").value(2))
                .andExpect(jsonPath("$.totalBalance").value(195000.50));
    }

    @Test
    void validWithdrawalCreatesNoticeAndReducesBalance() throws Exception {
        Long savings = productId("lerato.dlamini@example.com", ProductType.SAVINGS);

        mockMvc.perform(post("/api/withdrawals").with(httpBasic("lerato", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(body(savings, "1000.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").exists())
                .andExpect(jsonPath("$.balanceBefore").value(45000.50))
                .andExpect(jsonPath("$.balanceAfter").value(44000.50));

        mockMvc.perform(get("/api/withdrawals").with(httpBasic("lerato", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void retirementWithdrawalIsRejectedForInvestorUnder65() throws Exception {
        Long retirement = productId("lerato.dlamini@example.com", ProductType.RETIREMENT);

        mockMvc.perform(post("/api/withdrawals").with(httpBasic("lerato", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(body(retirement, "100.00")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("older than 65")));
    }

    @Test
    void retirementWithdrawalIsRejectedForInvestorExactly65() throws Exception {
        Long retirement = productId("sipho.nkosi@example.com", ProductType.RETIREMENT);

        mockMvc.perform(post("/api/withdrawals").with(httpBasic("sipho", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(body(retirement, "100.00")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void retirementWithdrawalIsAllowedForInvestorOver65() throws Exception {
        Long retirement = productId("thabo.mokoena@example.com", ProductType.RETIREMENT);

        mockMvc.perform(post("/api/withdrawals").with(httpBasic("thabo", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(body(retirement, "100000.00")))
                .andExpect(status().isCreated());
    }

    @Test
    void withdrawalOver90PercentIsRejected() throws Exception {
        Long savings = productId("lerato.dlamini@example.com", ProductType.SAVINGS); // balance 45000.50, max 40500.45

        mockMvc.perform(post("/api/withdrawals").with(httpBasic("lerato", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(body(savings, "40500.46")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("90%")));
    }

    @Test
    void invalidInputReturnsFieldErrors() throws Exception {
        Long savings = productId("lerato.dlamini@example.com", ProductType.SAVINGS);

        mockMvc.perform(post("/api/withdrawals").with(httpBasic("lerato", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(body(savings, "-5")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());

        mockMvc.perform(post("/api/withdrawals").with(httpBasic("lerato", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(body(savings, "10.123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());

        mockMvc.perform(post("/api/withdrawals").with(httpBasic("lerato", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.productId").exists());
    }

    @Test
    void investorCannotWithdrawFromSomeoneElsesProduct() throws Exception {
        Long thabosSavings = productId("thabo.mokoena@example.com", ProductType.SAVINGS);

        mockMvc.perform(post("/api/withdrawals").with(httpBasic("lerato", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(body(thabosSavings, "10.00")))
                .andExpect(status().isNotFound());
    }

    @Test
    void csvExportContainsHeaderAndFilteredRows() throws Exception {
        Long savings = productId("lerato.dlamini@example.com", ProductType.SAVINGS);
        mockMvc.perform(post("/api/withdrawals").with(httpBasic("lerato", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(body(savings, "500.00")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/withdrawals/export").param("productId", String.valueOf(savings))
                        .with(httpBasic("lerato", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("Reference,Date,Product")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tax-Free Savings")));

        // Filtering by a different product returns the header only
        Long retirement = productId("lerato.dlamini@example.com", ProductType.RETIREMENT);
        mockMvc.perform(get("/api/withdrawals/export").param("productId", String.valueOf(retirement))
                        .with(httpBasic("lerato", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Tax-Free Savings"))));
    }

    @Test
    void dateRangeWithFromAfterToIsRejected() throws Exception {
        mockMvc.perform(get("/api/withdrawals").param("from", "2026-10-02").param("to", "2026-10-01")
                        .with(httpBasic("lerato", PASSWORD)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/withdrawals").param("from", "not-a-date")
                        .with(httpBasic("lerato", PASSWORD)))
                .andExpect(status().isBadRequest());
    }
}
