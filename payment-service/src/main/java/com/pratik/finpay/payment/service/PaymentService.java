package com.pratik.finpay.payment.service;

import com.pratik.finpay.common.enums.PaymentEventType;
import com.pratik.finpay.common.enums.PaymentStatus;
import com.pratik.finpay.common.event.PaymentEvent;
import com.pratik.finpay.common.util.MoneyUtils;
import com.pratik.finpay.payment.client.WalletClient;
import com.pratik.finpay.payment.dto.request.TransferRequest;
import com.pratik.finpay.payment.dto.response.PaymentTransactionResponse;
import com.pratik.finpay.payment.dto.response.TransferResponse;
import com.pratik.finpay.payment.entity.PaymentTransaction;
import com.pratik.finpay.payment.event.PaymentEventPublisher;
import com.pratik.finpay.payment.exception.BusinessException;
import com.pratik.finpay.payment.exception.ResourceNotFoundException;
import com.pratik.finpay.payment.exception.WalletClientException;
import com.pratik.finpay.payment.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final DateTimeFormatter PAYMENT_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final WalletClient walletClient;
    private final IdempotencyStore idempotencyStore;
    private final PaymentEventPublisher paymentEventPublisher;

    public PaymentService(
            PaymentTransactionRepository paymentTransactionRepository,
            WalletClient walletClient,
            IdempotencyStore idempotencyStore,
            PaymentEventPublisher paymentEventPublisher
    ) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.walletClient = walletClient;
        this.idempotencyStore = idempotencyStore;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        validateTransferRequest(request);
        String idempotencyKey = normalizeIdempotencyKey(request.idempotencyKey());

        TransferResponse cachedResponse = findExistingByIdempotency(idempotencyKey);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        PaymentTransaction transaction = new PaymentTransaction(
                generatePaymentReference(),
                request.senderWalletId(),
                request.receiverWalletId(),
                MoneyUtils.requirePositive(request.amount(), "amount"),
                MoneyUtils.normalizeCurrency(request.currency()),
                idempotencyKey
        );
        transaction = paymentTransactionRepository.save(transaction);

        try {
            walletClient.debitWallet(
                    transaction.getSenderWalletId(),
                    transaction.getAmount(),
                    transaction.getCurrency(),
                    transaction.getPaymentReference()
            );
        } catch (WalletClientException ex) {
            transaction.markFailed(ex.getMessage());
            transaction = paymentTransactionRepository.save(transaction);
            rememberAndPublish(transaction, PaymentEventType.PAYMENT_FAILED, "Payment failed: " + ex.getMessage());
            return TransferResponse.from(transaction, "Payment failed: " + ex.getMessage());
        }

        try {
            walletClient.creditWallet(
                    transaction.getReceiverWalletId(),
                    transaction.getAmount(),
                    transaction.getCurrency(),
                    transaction.getPaymentReference()
            );
            transaction.markSuccess();
            transaction = paymentTransactionRepository.save(transaction);
            rememberAndPublish(transaction, PaymentEventType.PAYMENT_COMPLETED, "Payment completed successfully");
            return TransferResponse.from(transaction, "Payment completed successfully");
        } catch (WalletClientException creditFailure) {
            transaction = compensateAfterCreditFailure(transaction, creditFailure);
            String message = transaction.getStatus() == PaymentStatus.COMPENSATED
                    ? "Payment compensated after receiver credit failure"
                    : "Payment failed after debit and compensation also failed";
            PaymentEventType eventType = transaction.getStatus() == PaymentStatus.COMPENSATED
                    ? PaymentEventType.PAYMENT_COMPENSATED
                    : PaymentEventType.PAYMENT_FAILED;
            rememberAndPublish(transaction, eventType, message);
            return TransferResponse.from(transaction, message);
        }
    }

    @Transactional(readOnly = true)
    public PaymentTransactionResponse getPayment(String paymentReference) {
        return paymentTransactionRepository.findByPaymentReference(paymentReference)
                .map(PaymentTransactionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found"));
    }

    @Transactional(readOnly = true)
    public List<PaymentTransactionResponse> getWalletTransactions(Long walletId) {
        return paymentTransactionRepository
                .findBySenderWalletIdOrReceiverWalletIdOrderByCreatedAtDesc(walletId, walletId)
                .stream()
                .map(PaymentTransactionResponse::from)
                .toList();
    }

    private PaymentTransaction compensateAfterCreditFailure(
            PaymentTransaction transaction,
            WalletClientException creditFailure
    ) {
        try {
            walletClient.creditWallet(
                    transaction.getSenderWalletId(),
                    transaction.getAmount(),
                    transaction.getCurrency(),
                    transaction.getPaymentReference() + "-COMPENSATION"
            );
            transaction.markCompensated("Receiver credit failed, sender debit compensated: " + creditFailure.getMessage());
        } catch (WalletClientException compensationFailure) {
            transaction.markFailed(
                    "Receiver credit failed and compensation failed. Manual review required. Credit failure: "
                            + creditFailure.getMessage()
                            + "; compensation failure: "
                            + compensationFailure.getMessage()
            );
        }
        return paymentTransactionRepository.save(transaction);
    }

    private void validateTransferRequest(TransferRequest request) {
        if (request.senderWalletId().equals(request.receiverWalletId())) {
            throw new BusinessException("SAME_WALLET_TRANSFER", "Sender and receiver wallet must be different");
        }
        String currency = MoneyUtils.normalizeCurrency(request.currency());
        if (!MoneyUtils.isSupportedCurrency(currency)) {
            throw new BusinessException("UNSUPPORTED_CURRENCY", "Currency must be INR for v1");
        }
    }

    private TransferResponse findExistingByIdempotency(String idempotencyKey) {
        return idempotencyStore.getPaymentReference(idempotencyKey)
                .flatMap(paymentTransactionRepository::findByPaymentReference)
                .map(transaction -> TransferResponse.from(transaction, messageForStatus(transaction)))
                .orElseGet(() -> paymentTransactionRepository.findByIdempotencyKey(idempotencyKey)
                        .map(transaction -> {
                            idempotencyStore.savePaymentReference(idempotencyKey, transaction.getPaymentReference());
                            return TransferResponse.from(transaction, messageForStatus(transaction));
                        })
                        .orElse(null));
    }

    private String messageForStatus(PaymentTransaction transaction) {
        return switch (transaction.getStatus()) {
            case PENDING -> "Payment is pending";
            case SUCCESS -> "Payment completed successfully";
            case FAILED -> transaction.getFailureReason() == null ? "Payment failed" : transaction.getFailureReason();
            case COMPENSATED -> "Payment compensated after receiver credit failure";
        };
    }

    private void rememberAndPublish(PaymentTransaction transaction, PaymentEventType eventType, String message) {
        idempotencyStore.savePaymentReference(transaction.getIdempotencyKey(), transaction.getPaymentReference());
        safePublish(transaction, eventType, message);
    }

    private void safePublish(PaymentTransaction transaction, PaymentEventType eventType, String message) {
        try {
            paymentEventPublisher.publish(new PaymentEvent(
                    UUID.randomUUID().toString(),
                    transaction.getPaymentReference(),
                    transaction.getSenderWalletId(),
                    transaction.getReceiverWalletId(),
                    transaction.getAmount(),
                    transaction.getCurrency(),
                    transaction.getStatus(),
                    eventType,
                    message,
                    Instant.now()
            ));
        } catch (RuntimeException ignored) {
            // Payment status is already persisted. Event transport can be retried later.
        }
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        return idempotencyKey.trim();
    }

    private String generatePaymentReference() {
        String candidate;
        do {
            candidate = "PAY-" + LocalDate.now().format(PAYMENT_DATE_FORMAT) + "-"
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (paymentTransactionRepository.existsByPaymentReference(candidate));
        return candidate;
    }
}
