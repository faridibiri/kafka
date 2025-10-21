package com.example.kafka.consumer;

import com.example.kafka.model.*;
import com.example.kafka.producer.EventProducer;
import com.example.kafka.producer.NotificationProducer;
import com.example.kafka.producer.OrderProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationConsumer {

    private final OrderProducer orderProducer;
    private final EventProducer eventProducer;
    private final NotificationProducer notificationProducer;

    @KafkaListener(
            topics = "order.created",
            groupId = "validation-group",
            containerFactory = "validationKafkaListenerContainerFactory"
    )
    public void validateOrder(Order order) {
        log.info("🔍 Validating order: orderId={}", order.getOrderId());

        try {
            ValidationResult result = performValidation(order);

            if (result.isValid()) {
                order.setStatus(OrderStatus.VALIDATED);
                order.setUpdatedAt(LocalDateTime.now());

                log.info("✅ Order validation PASSED: orderId={}", order.getOrderId());

                orderProducer.sendOrderValidated(order);
                orderProducer.sendToInventory(order);

                OrderEvent event = OrderEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .orderId(order.getOrderId())
                        .eventType("ORDER_VALIDATED")
                        .previousStatus(OrderStatus.CONFIRMED)
                        .newStatus(OrderStatus.VALIDATED)
                        .description("Order validation successful")
                        .triggeredBy("ValidationConsumer")
                        .timestamp(LocalDateTime.now())
                        .build();

                eventProducer.publishEvent(event);

            } else {
                order.setStatus(OrderStatus.CANCELLED);
                order.setUpdatedAt(LocalDateTime.now());

                log.warn("⚠️ Order validation FAILED: orderId={}, reason={}",
                        order.getOrderId(), result.getReason());

                OrderEvent event = OrderEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .orderId(order.getOrderId())
                        .eventType("ORDER_VALIDATION_FAILED")
                        .previousStatus(OrderStatus.CONFIRMED)
                        .newStatus(OrderStatus.CANCELLED)
                        .description("Validation failed: " + result.getReason())
                        .triggeredBy("ValidationConsumer")
                        .timestamp(LocalDateTime.now())
                        .build();

                eventProducer.publishEvent(event);

                sendValidationFailedNotification(order, result.getReason());
            }

        } catch (Exception e) {
            log.error("❌ Error validating order: orderId={}, error={}",
                    order.getOrderId(), e.getMessage(), e);
        }
    }

    private ValidationResult performValidation(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return ValidationResult.invalid("Order must contain at least one item");
        }

        if (order.getCustomerEmail() == null || !order.getCustomerEmail().contains("@")) {
            return ValidationResult.invalid("Invalid customer email address");
        }

        if (order.getShippingAddress() == null || order.getShippingAddress().getStreet() == null) {
            return ValidationResult.invalid("Shipping address is required");
        }

        BigDecimal calculatedSubtotal = order.getItems().stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (order.getSubtotal().compareTo(calculatedSubtotal) != 0) {
            return ValidationResult.invalid("Subtotal mismatch");
        }

        boolean hasInvalidQuantity = order.getItems().stream()
                .anyMatch(item -> item.getQuantity() <= 0);

        if (hasInvalidQuantity) {
            return ValidationResult.invalid("Invalid item quantity");
        }

        if (order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.invalid("Total amount must be positive");
        }

        return ValidationResult.valid();
    }

    private void sendValidationFailedNotification(Order order, String reason) {
        Notification notification = Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .orderId(order.getOrderId())
                .customerId(order.getCustomerId())
                .recipient(order.getCustomerEmail())
                .type(NotificationType.ORDER_CANCELLED)
                .channel("EMAIL")
                .subject("Order Cancelled - Validation Failed")
                .message("Your order " + order.getOrderId() + " was cancelled: " + reason)
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        notificationProducer.sendNotification(notification);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ValidationResult {
        private boolean valid;
        private String reason;

        static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        static ValidationResult invalid(String reason) {
            return new ValidationResult(false, reason);
        }
    }
}
