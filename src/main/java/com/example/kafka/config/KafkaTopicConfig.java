package com.example.kafka.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name("order.created")
                .partitions(5)
                .replicas(1)
                .compact()
                .build();
    }

    @Bean
    public NewTopic orderValidatedTopic() {
        return TopicBuilder.name("order.validated")
                .partitions(5)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderPaymentTopic() {
        return TopicBuilder.name("order.payment")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderShippedTopic() {
        return TopicBuilder.name("order.shipped")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name("order.events")
                .partitions(5)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderNotificationsTopic() {
        return TopicBuilder.name("order.notifications")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderAnalyticsTopic() {
        return TopicBuilder.name("order.analytics")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderInventoryTopic() {
        return TopicBuilder.name("order.inventory")
                .partitions(5)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderDeadLetterTopic() {
        return TopicBuilder.name("order.dead-letter")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderRetryTopic() {
        return TopicBuilder.name("order.retry")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
