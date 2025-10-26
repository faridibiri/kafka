package com.example.kafka.consumer;

import com.example.kafka.model.Order;
import com.example.kafka.model.OrderEvent;
import com.example.kafka.model.OrderStatus;
import com.example.kafka.producer.EventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderConsumer {

    private final EventProducer eventProducer;

    @KafkaListener(
            topics = "order.created",
            groupId = "order-processing-group",
            containerFactory = "orderKafkaListenerContainerFactory"
    )
    public void consumeOrderCreated(
            @Payload Order order,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp,
            @Header(value = "priority", required = false) String priority
    ) {
        log.info("🔵 Received order from 'order.created': orderId={}, customer={}, partition={}, offset={}, priority={}",
                order.getOrderId(),
                order.getCustomerName(),
                partition,
                offset,
                priority);

        try {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setUpdatedAt(LocalDateTime.now());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("partition", partition);
            metadata.put("offset", offset);
            metadata.put("timestamp", timestamp);
            metadata.put("itemsCount", order.getItems().size());

            OrderEvent event = OrderEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(order.getOrderId())
                    .eventType("ORDER_CONFIRMED")
                    .previousStatus(OrderStatus.PENDING)
                    .newStatus(OrderStatus.CONFIRMED)
                    .description("Order received and confirmed")
                    .triggeredBy("OrderConsumer")
                    .timestamp(LocalDateTime.now())
                    .metadata(metadata)
                    .build();

            eventProducer.publishEvent(event);

            log.info("✅ Order confirmed successfully: orderId={}", order.getOrderId());

        } catch (Exception e) {
            log.error("❌ Error processing order: orderId={}, error={}",
                    order.getOrderId(), e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "order.shipped",
            groupId = "order-processing-group",
            containerFactory = "orderKafkaListenerContainerFactory"
    )
    public void consumeOrderShipped(ConsumerRecord<String, Order> record) {
        Order order = record.value();

        log.info("📦 Order shipped notification received: orderId={}, customer={}",
                order.getOrderId(), order.getCustomerName());

        OrderEvent event = OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(order.getOrderId())
                .eventType("ORDER_TRACKING_READY")
                .previousStatus(OrderStatus.READY_TO_SHIP)
                .newStatus(OrderStatus.SHIPPED)
                .description("Order shipped and tracking available")
                .triggeredBy("ShippingConsumer")
                .timestamp(LocalDateTime.now())
                .build();

        eventProducer.publishEvent(event);
    }
}
