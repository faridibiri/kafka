package com.example.kafka.consumer;

import com.example.kafka.model.Order;
import com.example.kafka.model.OrderEvent;
import com.example.kafka.model.OrderStatus;
import com.example.kafka.producer.EventProducer;
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
public class InventoryConsumer {

    private final OrderProducer orderProducer;
    private final EventProducer eventProducer;
    private final Random random = new Random();

    @KafkaListener(
            topics = "order.inventory",
            groupId = "inventory-group",
            containerFactory = "inventoryKafkaListenerContainerFactory"
    )
    public void checkInventory(Order order) {
        log.info("📊 Checking inventory for order: orderId={}, items={}",
                order.getOrderId(), order.getItems().size());

        try {
            simulateInventoryCheck();

            boolean inventoryAvailable = checkInventoryAvailability(order);

            if (inventoryAvailable) {
                order.setStatus(OrderStatus.INVENTORY_RESERVED);
                order.setUpdatedAt(LocalDateTime.now());

                log.info("✅ Inventory RESERVED: orderId={}", order.getOrderId());

                orderProducer.sendOrderToPayment(order);

                Map<String, Object> metadata = new HashMap<>();
                order.getItems().forEach(item -> {
                    metadata.put(item.getProductId(), item.getQuantity());
                });

                OrderEvent event = OrderEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .orderId(order.getOrderId())
                        .eventType("INVENTORY_RESERVED")
                        .previousStatus(OrderStatus.VALIDATED)
                        .newStatus(OrderStatus.INVENTORY_RESERVED)
                        .description("Inventory reserved successfully")
                        .triggeredBy("InventoryConsumer")
                        .timestamp(LocalDateTime.now())
                        .metadata(metadata)
                        .build();

                eventProducer.publishEvent(event);

            } else {
                order.setStatus(OrderStatus.CANCELLED);
                order.setUpdatedAt(LocalDateTime.now());

                log.warn("⚠️ Inventory NOT AVAILABLE: orderId={}", order.getOrderId());

                OrderEvent event = OrderEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .orderId(order.getOrderId())
                        .eventType("INVENTORY_UNAVAILABLE")
                        .previousStatus(OrderStatus.VALIDATED)
                        .newStatus(OrderStatus.CANCELLED)
                        .description("Inventory not available for order items")
                        .triggeredBy("InventoryConsumer")
                        .timestamp(LocalDateTime.now())
                        .build();

                eventProducer.publishEvent(event);
            }

        } catch (Exception e) {
            log.error("❌ Error checking inventory: orderId={}, error={}",
                    order.getOrderId(), e.getMessage(), e);
        }
    }

    private boolean checkInventoryAvailability(Order order) {
        return random.nextInt(10) < 9;
    }

    private void simulateInventoryCheck() {
        try {
            Thread.sleep(500 + random.nextInt(1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
