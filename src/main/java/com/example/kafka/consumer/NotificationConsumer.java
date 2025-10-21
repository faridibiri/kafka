package com.example.kafka.consumer;

import com.example.kafka.model.Notification;
import com.example.kafka.model.NotificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
public class NotificationConsumer {

    private final Random random = new Random();

    @KafkaListener(
            topics = "order.notifications",
            groupId = "notification-group",
            containerFactory = "notificationKafkaListenerContainerFactory"
    )
    public void sendNotification(Notification notification) {
        log.info("📧 Sending notification: type={}, channel={}, recipient={}",
                notification.getType(),
                notification.getChannel(),
                notification.getRecipient());

        try {
            simulateNotificationSending();

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());

            log.info("✅ Notification SENT: notificationId={}, type={}, recipient={}",
                    notification.getNotificationId(),
                    notification.getType(),
                    notification.getRecipient());

        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            log.error("❌ Failed to send notification: notificationId={}, error={}",
                    notification.getNotificationId(), e.getMessage());
        }
    }

    private void simulateNotificationSending() {
        try {
            Thread.sleep(200 + random.nextInt(500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
