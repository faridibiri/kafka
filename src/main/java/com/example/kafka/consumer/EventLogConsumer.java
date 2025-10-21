package com.example.kafka.consumer;

import com.example.kafka.model.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EventLogConsumer {

    @KafkaListener(
            topics = "order.events",
            groupId = "event-logging-group",
            containerFactory = "eventKafkaListenerContainerFactory"
    )
    public void logEvents(
            List<OrderEvent> events,
            @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions
    ) {
        log.info("📝 Batch processing {} events from order.events", events.size());

        for (int i = 0; i < events.size(); i++) {
            OrderEvent event = events.get(i);
            int partition = partitions.get(i);

            log.info("📋 Event Log: eventId={}, type={}, orderId={}, status: {} -> {}, partition={}",
                    event.getEventId(),
                    event.getEventType(),
                    event.getOrderId(),
                    event.getPreviousStatus(),
                    event.getNewStatus(),
                    partition);
        }

        log.info("✅ Successfully logged {} events", events.size());
    }
}
