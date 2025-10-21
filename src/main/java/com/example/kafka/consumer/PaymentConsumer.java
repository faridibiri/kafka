package com.example.kafka.consumer;

import com.example.kafka.model.*;
import com.example.kafka.producer.EventProducer;
import com.example.kafka.producer.NotificationProducer;
import com.example.kafka.producer.OrderProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentConsumer {

    private final OrderProducer orderProducer;
    private final EventProducer eventProducer;
    private final NotificationProducer notificationProducer;
    private final Random random = new Random();

    @KafkaListener(
            topics = "order.payment",
            groupId = "payment-group",
            containerFactory = "paymentKafkaListenerContainerFactory"
    )
    public void processPayment(Order order) {
        log.info("💳 Processing payment for order: orderId={}, amount={}, method={}",
                order.getOrderId(),
                order.getTotalAmount(),
                order.getPaymentInfo().getPaymentMethod());

        try {
            order.setStatus(OrderStatus.PAYMENT_PROCESSING);
            order.setUpdatedAt(LocalDateTime.now());

            simulatePaymentProcessing();

            boolean paymentSuccess = processPaymentTransaction(order);

            if (paymentSuccess) {
                handleSuccessfulPayment(order);
            } else {
                handleFailedPayment(order);
            }

        } catch (Exception e) {
            log.error("❌ Error processing payment: orderId={}, error={}",
                    order.getOrderId(), e.getMessage(), e);
            handleFailedPayment(order);
        }
    }

    private void handleSuccessfulPayment(Order order) {
        order.setStatus(OrderStatus.PAYMENT_COMPLETED);
        order.setUpdatedAt(LocalDateTime.now());

        PaymentInfo paymentInfo = order.getPaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.CAPTURED);
        paymentInfo.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        log.info("✅ Payment SUCCESSFUL: orderId={}, transactionId={}",
                order.getOrderId(), paymentInfo.getTransactionId());

        orderProducer.sendOrderShipped(order);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("transactionId", paymentInfo.getTransactionId());
        metadata.put("amount", order.getTotalAmount());
        metadata.put("paymentMethod", paymentInfo.getPaymentMethod());

        OrderEvent event = OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(order.getOrderId())
                .eventType("PAYMENT_COMPLETED")
                .previousStatus(OrderStatus.PAYMENT_PROCESSING)
                .newStatus(OrderStatus.PAYMENT_COMPLETED)
                .description("Payment processed successfully")
                .triggeredBy("PaymentConsumer")
                .timestamp(LocalDateTime.now())
                .metadata(metadata)
                .build();

        eventProducer.publishEvent(event);

        sendPaymentSuccessNotification(order);
    }

    private void handleFailedPayment(Order order) {
        order.setStatus(OrderStatus.PAYMENT_FAILED);
        order.setUpdatedAt(LocalDateTime.now());

        PaymentInfo paymentInfo = order.getPaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.DECLINED);

        log.warn("⚠️ Payment FAILED: orderId={}", order.getOrderId());

        OrderEvent event = OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(order.getOrderId())
                .eventType("PAYMENT_FAILED")
                .previousStatus(OrderStatus.PAYMENT_PROCESSING)
                .newStatus(OrderStatus.PAYMENT_FAILED)
                .description("Payment processing failed")
                .triggeredBy("PaymentConsumer")
                .timestamp(LocalDateTime.now())
                .build();

        eventProducer.publishEvent(event);

        sendPaymentFailedNotification(order);
    }

    private boolean processPaymentTransaction(Order order) {
        if (order.getTotalAmount().doubleValue() > 10000) {
            log.warn("⚠️ High-value transaction requires manual approval: orderId={}", order.getOrderId());
            return false;
        }
        return random.nextInt(10) < 9;
    }

    private void simulatePaymentProcessing() {
        try {
            Thread.sleep(1000 + random.nextInt(2000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendPaymentSuccessNotification(Order order) {
        Notification notification = Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .orderId(order.getOrderId())
                .customerId(order.getCustomerId())
                .recipient(order.getCustomerEmail())
                .type(NotificationType.PAYMENT_SUCCESS)
                .channel("EMAIL")
                .subject("Payment Successful")
                .message("Your payment of " + order.getTotalAmount() + " has been processed successfully.")
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        notificationProducer.sendNotification(notification);
    }

    private void sendPaymentFailedNotification(Order order) {
        Notification notification = Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .orderId(order.getOrderId())
                .customerId(order.getCustomerId())
                .recipient(order.getCustomerEmail())
                .type(NotificationType.PAYMENT_FAILED)
                .channel("EMAIL")
                .subject("Payment Failed")
                .message("Payment processing failed for order " + order.getOrderId())
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        notificationProducer.sendNotification(notification);
    }
}
