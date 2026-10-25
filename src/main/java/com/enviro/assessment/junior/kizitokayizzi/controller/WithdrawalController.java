package com.enviro.assessment.junior.kizitokayizzi.controller;

import com.enviro.assessment.junior.kizitokayizzi.dto.WithdrawalRequest;
import com.enviro.assessment.junior.kizitokayizzi.dto.WithdrawalResponse;
import com.enviro.assessment.junior.kizitokayizzi.security.InvestorUserDetails;
import com.enviro.assessment.junior.kizitokayizzi.service.WithdrawalService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/withdrawals")
public class WithdrawalController {

    private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final WithdrawalService withdrawalService;

    public WithdrawalController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WithdrawalResponse create(@AuthenticationPrincipal InvestorUserDetails user,
                                     @Valid @RequestBody WithdrawalRequest request) {
        return withdrawalService.createNotice(user.getInvestorId(), request);
    }

    @GetMapping
    public List<WithdrawalResponse> history(
            @AuthenticationPrincipal InvestorUserDetails user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long productId) {
        return withdrawalService.history(user.getInvestorId(), from, to, productId);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @AuthenticationPrincipal InvestorUserDetails user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long productId) {
        String csv = withdrawalService.exportCsv(user.getInvestorId(), from, to, productId);
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("withdrawal-statement.csv").build().toString())
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
