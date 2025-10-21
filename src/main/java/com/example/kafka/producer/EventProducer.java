package com.example.kafka.producer;

import com.example.kafka.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProducer {

    private final KafkaTemplate<String, OrderEvent> eventKafkaTemplate;

    public void publishEvent(OrderEvent event) {
        log.info("📢 Publishing event: type={}, orderId={}, status: {} -> {}",
                event.getEventType(),
                event.getOrderId(),
                event.getPreviousStatus(),
                event.getNewStatus());

        eventKafkaTemplate.send("order.events", event.getOrderId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("✅ Event published: eventId={}, partition={}",
                                event.getEventId(), result.getRecordMetadata().partition());
                    } else {
                        log.error("❌ Failed to publish event: {}", ex.getMessage());
                    }
                });
    }
}
