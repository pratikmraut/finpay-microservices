package com.pratik.finpay.payment.controller;

import com.pratik.finpay.common.dto.ApiResponse;
import com.pratik.finpay.payment.dto.request.TransferRequest;
import com.pratik.finpay.payment.dto.response.PaymentTransactionResponse;
import com.pratik.finpay.payment.dto.response.TransferResponse;
import com.pratik.finpay.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payments/transfer")
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Transfer processed", paymentService.transfer(request)));
    }

    @GetMapping("/payments/{paymentReference}")
    public ResponseEntity<ApiResponse<PaymentTransactionResponse>> getPayment(@PathVariable String paymentReference) {
        return ResponseEntity.ok(ApiResponse.success("Payment fetched successfully", paymentService.getPayment(paymentReference)));
    }

    @GetMapping("/wallets/{walletId}/transactions")
    public ResponseEntity<ApiResponse<List<PaymentTransactionResponse>>> getWalletTransactions(@PathVariable Long walletId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Wallet transactions fetched successfully",
                paymentService.getWalletTransactions(walletId)
        ));
    }
}
