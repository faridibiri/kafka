package com.example.kafka.controller;

import com.example.kafka.model.*;
import com.example.kafka.producer.OrderProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderProducer orderProducer;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Order order) {
        try {
            order.setOrderId(UUID.randomUUID().toString());
            order.setStatus(OrderStatus.PENDING);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());

            BigDecimal subtotal = order.getItems().stream()
                    .map(OrderItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setSubtotal(subtotal);

            if (order.getTaxAmount() == null) {
                order.setTaxAmount(subtotal.multiply(new BigDecimal("0.20")));
            }

            if (order.getShippingCost() == null) {
                order.setShippingCost(new BigDecimal("10.00"));
            }

            BigDecimal total = subtotal
                    .add(order.getTaxAmount())
                    .add(order.getShippingCost());

            if (order.getDiscountAmount() != null) {
                total = total.subtract(order.getDiscountAmount());
            }

            order.setTotalAmount(total);

            if (order.getPaymentInfo() == null) {
                order.setPaymentInfo(PaymentInfo.builder()
                        .paymentMethod("CREDIT_CARD")
                        .paymentStatus(PaymentStatus.PENDING)
                        .build());
            }

            if (order.getPriority() == null) {
                order.setPriority(OrderPriority.NORMAL);
            }

            log.info("🆕 Creating new order: orderId={}, customer={}, total={}",
                    order.getOrderId(), order.getCustomerName(), order.getTotalAmount());

            orderProducer.sendOrderCreated(order);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.getOrderId());
            response.put("status", "PENDING");
            response.put("totalAmount", order.getTotalAmount());
            response.put("message", "Order created successfully and sent for processing");
            response.put("estimatedDelivery", LocalDateTime.now().plusDays(5).toString());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("❌ Error creating order: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create order");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/example")
    public ResponseEntity<Order> getExampleOrder() {
        Order exampleOrder = Order.builder()
                .customerId("CUST-" + UUID.randomUUID().toString().substring(0, 8))
                .customerName("Marie Dubois")
                .customerEmail("marie.dubois@example.com")
                .phoneNumber("+33 6 12 34 56 78")
                .priority(OrderPriority.NORMAL)
                .items(Arrays.asList(
                        OrderItem.builder()
                                .itemId(UUID.randomUUID().toString())
                                .productId("PROD-001")
                                .productName("MacBook Pro 14\"")
                                .sku("MBP14-256-SG")
                                .quantity(1)
                                .unitPrice(new BigDecimal("2499.99"))
                                .totalPrice(new BigDecimal("2499.99"))
                                .category("Electronics")
                                .weight(1.6)
                                .build(),
                        OrderItem.builder()
                                .itemId(UUID.randomUUID().toString())
                                .productId("PROD-002")
                                .productName("Magic Mouse")
                                .sku("MM-WHT")
                                .quantity(1)
                                .unitPrice(new BigDecimal("99.99"))
                                .totalPrice(new BigDecimal("99.99"))
                                .category("Accessories")
                                .weight(0.1)
                                .build(),
                        OrderItem.builder()
                                .itemId(UUID.randomUUID().toString())
                                .productId("PROD-003")
                                .productName("USB-C Cable")
                                .sku("USBC-2M")
                                .quantity(2)
                                .unitPrice(new BigDecimal("19.99"))
                                .totalPrice(new BigDecimal("39.98"))
                                .category("Accessories")
                                .weight(0.05)
                                .build()
                ))
                .shippingAddress(Address.builder()
                        .street("42 Avenue des Champs-Élysées")
                        .city("Paris")
                        .state("Île-de-France")
                        .postalCode("75008")
                        .country("France")
                        .phoneNumber("+33 6 12 34 56 78")
                        .build())
                .billingAddress(Address.builder()
                        .street("42 Avenue des Champs-Élysées")
                        .city("Paris")
                        .state("Île-de-France")
                        .postalCode("75008")
                        .country("France")
                        .phoneNumber("+33 6 12 34 56 78")
                        .build())
                .paymentInfo(PaymentInfo.builder()
                        .paymentMethod("CREDIT_CARD")
                        .cardLastFour("1234")
                        .paymentProcessor("Stripe")
                        .paymentStatus(PaymentStatus.PENDING)
                        .build())
                .notes("Please deliver during business hours")
                .build();

        return ResponseEntity.ok(exampleOrder);
    }

    @GetMapping("/example/high-value")
    public ResponseEntity<Order> getHighValueOrder() {
        Order order = Order.builder()
                .customerId("CUST-VIP-" + UUID.randomUUID().toString().substring(0, 6))
                .customerName("Jean Dupont")
                .customerEmail("jean.dupont@example.com")
                .phoneNumber("+33 6 98 76 54 32")
                .priority(OrderPriority.EXPRESS)
                .items(Arrays.asList(
                        OrderItem.builder()
                                .itemId(UUID.randomUUID().toString())
                                .productId("PROD-PREMIUM-001")
                                .productName("MacBook Pro 16\" Max")
                                .sku("MBP16-1TB-MAX")
                                .quantity(3)
                                .unitPrice(new BigDecimal("3999.99"))
                                .totalPrice(new BigDecimal("11999.97"))
                                .category("Electronics")
                                .weight(2.1)
                                .build()
                ))
                .shippingAddress(Address.builder()
                        .street("15 Rue de la Paix")
                        .city("Lyon")
                        .state("Auvergne-Rhône-Alpes")
                        .postalCode("69001")
                        .country("France")
                        .phoneNumber("+33 6 98 76 54 32")
                        .build())
                .billingAddress(Address.builder()
                        .street("15 Rue de la Paix")
                        .city("Lyon")
                        .state("Auvergne-Rhône-Alpes")
                        .postalCode("69001")
                        .country("France")
                        .phoneNumber("+33 6 98 76 54 32")
                        .build())
                .paymentInfo(PaymentInfo.builder()
                        .paymentMethod("CREDIT_CARD")
                        .cardLastFour("9876")
                        .paymentProcessor("Stripe")
                        .paymentStatus(PaymentStatus.PENDING)
                        .build())
                .couponCode("VIP20")
                .build();

        return ResponseEntity.ok(order);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Kafka Order Management System");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }
}
