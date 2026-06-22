package com.pratik.finpay.payment.event;

import com.pratik.finpay.common.event.PaymentEvent;

public interface PaymentEventPublisher {

    void publish(PaymentEvent event);
}
