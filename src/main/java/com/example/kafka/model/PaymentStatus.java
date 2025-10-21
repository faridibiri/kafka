package com.example.kafka.model;

public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    DECLINED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    FAILED
}
