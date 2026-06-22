package com.pratik.finpay.wallet.controller;

import com.pratik.finpay.common.dto.ApiResponse;
import com.pratik.finpay.wallet.dto.request.CreateWalletRequest;
import com.pratik.finpay.wallet.dto.request.UpdateWalletStatusRequest;
import com.pratik.finpay.wallet.dto.request.WalletOperationRequest;
import com.pratik.finpay.wallet.dto.response.WalletBalanceResponse;
import com.pratik.finpay.wallet.dto.response.WalletResponse;
import com.pratik.finpay.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/api/v1/wallets")
    public ResponseEntity<ApiResponse<WalletResponse>> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        WalletResponse response = walletService.createWallet(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Wallet created successfully", response));
    }

    @GetMapping("/api/v1/wallets/{walletId}")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(@PathVariable Long walletId) {
        return ResponseEntity.ok(ApiResponse.success("Wallet fetched successfully", walletService.getWallet(walletId)));
    }

    @GetMapping("/api/v1/wallets/{walletId}/balance")
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> getBalance(@PathVariable Long walletId) {
        return ResponseEntity.ok(ApiResponse.success("Wallet balance fetched successfully", walletService.getBalance(walletId)));
    }

    @PatchMapping("/api/v1/wallets/{walletId}/status")
    public ResponseEntity<ApiResponse<WalletResponse>> updateStatus(
            @PathVariable Long walletId,
            @Valid @RequestBody UpdateWalletStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Wallet status updated successfully", walletService.updateStatus(walletId, request)));
    }

    @PostMapping("/internal/v1/wallets/{walletId}/debit")
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> debitWallet(
            @PathVariable Long walletId,
            @Valid @RequestBody WalletOperationRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Wallet debited successfully", walletService.debitWallet(walletId, request)));
    }

    @PostMapping("/internal/v1/wallets/{walletId}/credit")
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> creditWallet(
            @PathVariable Long walletId,
            @Valid @RequestBody WalletOperationRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Wallet credited successfully", walletService.creditWallet(walletId, request)));
    }
}
