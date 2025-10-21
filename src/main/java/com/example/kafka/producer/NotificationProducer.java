package com.example.kafka.producer;

import com.example.kafka.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, Notification> notificationKafkaTemplate;

    @Async
    public void sendNotification(Notification notification) {
        log.info("🔔 Sending notification: type={}, recipient={}, orderId={}",
                notification.getType(),
                notification.getRecipient(),
                notification.getOrderId());

        notificationKafkaTemplate.send("order.notifications", notification.getNotificationId(), notification)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("✅ Notification sent: notificationId={}, offset={}",
                                notification.getNotificationId(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("❌ Failed to send notification: {}", ex.getMessage());
                    }
                });
    }
}
