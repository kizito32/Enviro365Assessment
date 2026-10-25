package com.enviro.assessment.junior.kizitokayizzi.service;

import com.enviro.assessment.junior.kizitokayizzi.dto.WithdrawalResponse;
import com.enviro.assessment.junior.kizitokayizzi.entity.ProductType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvStatementWriterTest {

    @Test
    void writesHeaderAndOneRowPerWithdrawal() {
        WithdrawalResponse row = new WithdrawalResponse(1L, "WN-ABC12345", 3L, "Tax-Free Savings",
                ProductType.SAVINGS, new BigDecimal("250.00"), new BigDecimal("1000.00"),
                new BigDecimal("750.00"), LocalDateTime.of(2026, 10, 25, 10, 0, 0));

        String csv = CsvStatementWriter.write(List.of(row));

        assertThat(csv.split("\r\n")).containsExactly(
                "Reference,Date,Product,Product Type,Amount,Balance Before,Balance After",
                "WN-ABC12345,2026-10-25 10:00:00,Tax-Free Savings,SAVINGS,250.00,1000.00,750.00");
    }

    @Test
    void quotesFieldsContainingCommasAndQuotes() {
        WithdrawalResponse row = new WithdrawalResponse(1L, "WN-1", 3L, "Savings, \"Plus\"",
                ProductType.SAVINGS, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO,
                LocalDateTime.of(2026, 1, 1, 0, 0, 0));

        assertThat(CsvStatementWriter.write(List.of(row))).contains("\"Savings, \"\"Plus\"\"\"");
    }

    @Test
    void emptyStatementContainsOnlyTheHeader() {
        assertThat(CsvStatementWriter.write(List.of()).split("\r\n")).hasSize(1);
    }
}
