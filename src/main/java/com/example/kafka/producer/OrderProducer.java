package com.example.kafka.producer;

import com.example.kafka.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, Order> orderKafkaTemplate;

    public void sendOrderCreated(Order order) {
        log.info("📤 Sending order to 'order.created': orderId={}, customer={}, total={}",
                order.getOrderId(), order.getCustomerName(), order.getTotalAmount());

        ProducerRecord<String, Order> record = new ProducerRecord<>(
                "order.created",
                null,
                order.getOrderId(),
                order
        );

        record.headers().add(new RecordHeader("event-type", "ORDER_CREATED".getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("priority", order.getPriority().name().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("customer-id", order.getCustomerId().getBytes(StandardCharsets.UTF_8)));

        CompletableFuture<SendResult<String, Order>> future = orderKafkaTemplate.send(record);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("✅ Order sent successfully: orderId={}, partition={}, offset={}",
                        order.getOrderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("❌ Failed to send order: orderId={}, error={}",
                        order.getOrderId(), ex.getMessage(), ex);
            }
        });
    }

    public void sendOrderValidated(Order order) {
        log.info("📤 Sending validated order: orderId={}", order.getOrderId());

        orderKafkaTemplate.send("order.validated", order.getOrderId(), order)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("✅ Validated order sent: orderId={}, offset={}",
                                order.getOrderId(), result.getRecordMetadata().offset());
                    } else {
                        log.error("❌ Failed to send validated order: {}", ex.getMessage());
                    }
                });
    }

    public void sendOrderToPayment(Order order) {
        log.info("📤 Sending order to payment: orderId={}, amount={}",
                order.getOrderId(), order.getTotalAmount());

        orderKafkaTemplate.send("order.payment", order.getOrderId(), order)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("✅ Order sent to payment: orderId={}", order.getOrderId());
                    } else {
                        log.error("❌ Failed to send to payment: {}", ex.getMessage());
                    }
                });
    }

    public void sendOrderShipped(Order order) {
        log.info("📤 Sending shipped order: orderId={}", order.getOrderId());

        ProducerRecord<String, Order> record = new ProducerRecord<>(
                "order.shipped",
                order.getOrderId(),
                order
        );

        record.headers().add(new RecordHeader("tracking-enabled", "true".getBytes(StandardCharsets.UTF_8)));

        orderKafkaTemplate.send(record)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("✅ Shipped order sent: orderId={}", order.getOrderId());
                    } else {
                        log.error("❌ Failed to send shipped order: {}", ex.getMessage());
                    }
                });
    }

    public void sendToInventory(Order order) {
        log.info("📤 Sending order to inventory: orderId={}, items={}",
                order.getOrderId(), order.getItems().size());

        orderKafkaTemplate.send("order.inventory", order.getOrderId(), order)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("✅ Order sent to inventory: orderId={}", order.getOrderId());
                    } else {
                        log.error("❌ Failed to send to inventory: {}", ex.getMessage());
                    }
                });
    }
}
