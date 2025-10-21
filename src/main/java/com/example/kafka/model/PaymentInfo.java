package com.example.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInfo {
    private String paymentMethod;
    private String transactionId;
    private PaymentStatus paymentStatus;
    private String cardLastFour;
    private String paymentProcessor;
}
