package com.pratik.finpay.payment.service;

import com.pratik.finpay.common.enums.PaymentEventType;
import com.pratik.finpay.common.enums.PaymentStatus;
import com.pratik.finpay.common.event.PaymentEvent;
import com.pratik.finpay.payment.client.WalletBalanceResponse;
import com.pratik.finpay.payment.client.WalletClient;
import com.pratik.finpay.payment.dto.request.TransferRequest;
import com.pratik.finpay.payment.dto.response.TransferResponse;
import com.pratik.finpay.payment.entity.PaymentTransaction;
import com.pratik.finpay.payment.event.PaymentEventPublisher;
import com.pratik.finpay.payment.exception.BusinessException;
import com.pratik.finpay.payment.exception.WalletClientException;
import com.pratik.finpay.payment.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private WalletClient walletClient;

    @Mock
    private IdempotencyStore idempotencyStore;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentTransactionRepository,
                walletClient,
                idempotencyStore,
                paymentEventPublisher
        );
    }

    @Test
    void successfulTransferShouldDebitCreditSaveSuccessAndPublishEvent() {
        prepareNewPayment();
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> withIdentity(invocation.getArgument(0)));
        when(walletClient.debitWallet(eq(1L), any(BigDecimal.class), eq("INR"), any()))
                .thenReturn(walletBalance(1L, "4500.00"));
        when(walletClient.creditWallet(eq(2L), any(BigDecimal.class), eq("INR"), any()))
                .thenReturn(walletBalance(2L, "1500.00"));

        TransferResponse response = paymentService.transfer(request("idem-12345"));

        assertEquals(PaymentStatus.SUCCESS, response.status());
        verify(walletClient).debitWallet(eq(1L), eq(new BigDecimal("500.00")), eq("INR"), any());
        verify(walletClient).creditWallet(eq(2L), eq(new BigDecimal("500.00")), eq("INR"), any());
        verify(idempotencyStore).savePaymentReference(eq("idem-12345"), any());

        ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(paymentEventPublisher).publish(eventCaptor.capture());
        assertEquals(PaymentEventType.PAYMENT_COMPLETED, eventCaptor.getValue().eventType());
    }

    @Test
    void duplicateIdempotencyKeyShouldReturnExistingTransactionAndNotCallWalletService() {
        PaymentTransaction existing = existingTransaction(PaymentStatus.SUCCESS);
        when(idempotencyStore.getPaymentReference("idem-12345")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByIdempotencyKey("idem-12345")).thenReturn(Optional.of(existing));

        TransferResponse response = paymentService.transfer(request("idem-12345"));

        assertEquals("PAY-EXISTING", response.paymentReference());
        assertEquals(PaymentStatus.SUCCESS, response.status());
        verify(walletClient, never()).debitWallet(any(), any(), any(), any());
        verify(walletClient, never()).creditWallet(any(), any(), any(), any());
        verify(idempotencyStore).savePaymentReference("idem-12345", "PAY-EXISTING");
    }

    @Test
    void sameWalletTransferShouldFail() {
        TransferRequest request = new TransferRequest(1L, 1L, new BigDecimal("500.00"), "INR", "idem-12345");

        BusinessException exception = assertThrows(BusinessException.class, () -> paymentService.transfer(request));

        assertEquals("SAME_WALLET_TRANSFER", exception.getErrorCode());
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
    }

    @Test
    void debitFailureShouldMarkFailedAndPublishFailedEvent() {
        prepareNewPayment();
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> withIdentity(invocation.getArgument(0)));
        when(walletClient.debitWallet(eq(1L), any(BigDecimal.class), eq("INR"), any()))
                .thenThrow(new WalletClientException("INSUFFICIENT_BALANCE", "Wallet does not have sufficient balance"));

        TransferResponse response = paymentService.transfer(request("idem-12345"));

        assertEquals(PaymentStatus.FAILED, response.status());
        verify(walletClient, never()).creditWallet(eq(2L), any(), any(), any());

        ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(paymentEventPublisher).publish(eventCaptor.capture());
        assertEquals(PaymentEventType.PAYMENT_FAILED, eventCaptor.getValue().eventType());
    }

    @Test
    void creditFailureAfterDebitShouldTriggerCompensation() {
        prepareNewPayment();
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> withIdentity(invocation.getArgument(0)));
        when(walletClient.debitWallet(eq(1L), any(BigDecimal.class), eq("INR"), any()))
                .thenReturn(walletBalance(1L, "4500.00"));
        when(walletClient.creditWallet(eq(2L), any(BigDecimal.class), eq("INR"), any()))
                .thenThrow(new WalletClientException("RECEIVER_WALLET_NOT_FOUND", "Receiver wallet not found"));
        when(walletClient.creditWallet(eq(1L), any(BigDecimal.class), eq("INR"), any()))
                .thenReturn(walletBalance(1L, "5000.00"));

        TransferResponse response = paymentService.transfer(request("idem-12345"));

        assertEquals(PaymentStatus.COMPENSATED, response.status());
        verify(walletClient).creditWallet(eq(1L), eq(new BigDecimal("500.00")), eq("INR"), any());

        ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(paymentEventPublisher).publish(eventCaptor.capture());
        assertEquals(PaymentEventType.PAYMENT_COMPENSATED, eventCaptor.getValue().eventType());
    }

    @Test
    void eventPublishFailureShouldNotCorruptPaymentStatus() {
        prepareNewPayment();
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> withIdentity(invocation.getArgument(0)));
        when(walletClient.debitWallet(eq(1L), any(BigDecimal.class), eq("INR"), any()))
                .thenReturn(walletBalance(1L, "4500.00"));
        when(walletClient.creditWallet(eq(2L), any(BigDecimal.class), eq("INR"), any()))
                .thenReturn(walletBalance(2L, "1500.00"));
        doThrow(new RuntimeException("event transport down")).when(paymentEventPublisher).publish(any(PaymentEvent.class));

        TransferResponse response = paymentService.transfer(request("idem-12345"));

        assertEquals(PaymentStatus.SUCCESS, response.status());
    }

    private void prepareNewPayment() {
        when(idempotencyStore.getPaymentReference("idem-12345")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByIdempotencyKey("idem-12345")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.existsByPaymentReference(any())).thenReturn(false);
    }

    private TransferRequest request(String idempotencyKey) {
        return new TransferRequest(1L, 2L, new BigDecimal("500.00"), "INR", idempotencyKey);
    }

    private PaymentTransaction existingTransaction(PaymentStatus status) {
        PaymentTransaction transaction = new PaymentTransaction(
                "PAY-EXISTING",
                1L,
                2L,
                new BigDecimal("500.00"),
                "INR",
                "idem-12345"
        );
        if (status == PaymentStatus.SUCCESS) {
            transaction.markSuccess();
        }
        return withIdentity(transaction);
    }

    private PaymentTransaction withIdentity(PaymentTransaction transaction) {
        ReflectionTestUtils.setField(transaction, "id", 1L);
        ReflectionTestUtils.setField(transaction, "createdAt", Instant.parse("2026-06-21T00:00:00Z"));
        ReflectionTestUtils.setField(transaction, "updatedAt", Instant.parse("2026-06-21T00:00:00Z"));
        return transaction;
    }

    private WalletBalanceResponse walletBalance(Long walletId, String balance) {
        return new WalletBalanceResponse(walletId, "WALLET-" + walletId, new BigDecimal(balance), "INR", null);
    }
}
