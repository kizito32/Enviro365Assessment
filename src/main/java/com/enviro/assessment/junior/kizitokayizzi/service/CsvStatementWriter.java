package com.enviro.assessment.junior.kizitokayizzi.service;

import com.enviro.assessment.junior.kizitokayizzi.dto.WithdrawalResponse;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Builds a CSV statement by hand; the format is small enough that a library would be overkill. */
final class CsvStatementWriter {

    private static final String HEADER = "Reference,Date,Product,Product Type,Amount,Balance Before,Balance After";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private CsvStatementWriter() {
    }

    static String write(List<WithdrawalResponse> rows) {
        StringBuilder csv = new StringBuilder(HEADER).append("\r\n");
        for (WithdrawalResponse row : rows) {
            csv.append(escape(row.reference())).append(',')
                    .append(escape(row.createdAt().format(DATE_TIME))).append(',')
                    .append(escape(row.productName())).append(',')
                    .append(escape(row.productType().name())).append(',')
                    .append(row.amount().toPlainString()).append(',')
                    .append(row.balanceBefore().toPlainString()).append(',')
                    .append(row.balanceAfter().toPlainString()).append("\r\n");
        }
        return csv.toString();
    }

    // RFC 4180: quote fields containing commas, quotes or line breaks, and double any quotes inside
    private static String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
