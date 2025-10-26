package com.example.kafka.streams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class OrderAnalyticsStreams {

    private final ObjectMapper objectMapper;

    public OrderAnalyticsStreams() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Autowired
    public void buildOrderStatusStream(StreamsBuilder streamsBuilder) {
        KStream<String, String> orderEventsStream = streamsBuilder
                .stream("order.events", Consumed.with(Serdes.String(), Serdes.String()));

        countOrdersByStatus(orderEventsStream);
        log.info("🔧 Kafka Streams: Order status counting configured");
    }

    @Autowired
    public void buildOrderValueStream(StreamsBuilder streamsBuilder) {
        KStream<String, String> orderCreatedStream = streamsBuilder
                .stream("order.created", Consumed.with(Serdes.String(), Serdes.String()));

        detectHighValueOrders(orderCreatedStream);
        calculateTotalRevenue(orderCreatedStream);
        log.info("🔧 Kafka Streams: Order value analysis configured");
    }

    @Autowired
    public void buildCustomerAnalyticsStream(StreamsBuilder streamsBuilder) {
        KStream<String, String> orderCreatedStream = streamsBuilder
                .stream("order.created", Consumed.with(Serdes.String(), Serdes.String()));

        countOrdersByCustomer(orderCreatedStream);
        log.info("🔧 Kafka Streams: Customer analytics configured");
    }

    @Autowired
    public void buildProductAnalyticsStream(StreamsBuilder streamsBuilder) {
        KStream<String, String> orderCreatedStream = streamsBuilder
                .stream("order.created", Consumed.with(Serdes.String(), Serdes.String()));

        analyzePopularProducts(orderCreatedStream);
        log.info("🔧 Kafka Streams: Product analytics configured");
    }

    private void countOrdersByStatus(KStream<String, String> orderEventsStream) {
        orderEventsStream
                .mapValues(this::parseJson)
                .filter((key, event) -> event != null && event.has("newStatus"))
                .groupBy(
                        (key, event) -> event.get("newStatus").asText(),
                        Grouped.with(Serdes.String(), new JsonSerde<>(JsonNode.class, objectMapper))
                )
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
                .count(Materialized.as("order-status-counts-store"))
                .toStream()
                .map((windowedKey, count) -> {
                    String status = windowedKey.key();
                    String windowStart = windowedKey.window().startTime().toString();
                    String windowEnd = windowedKey.window().endTime().toString();

                    Map<String, Object> analytics = new HashMap<>();
                    analytics.put("status", status);
                    analytics.put("count", count);
                    analytics.put("windowStart", windowStart);
                    analytics.put("windowEnd", windowEnd);
                    analytics.put("type", "STATUS_COUNT");

                    log.info("📊 Status Analytics: status={}, count={}, window=[{} - {}]",
                            status, count, windowStart, windowEnd);

                    try {
                        return KeyValue.pair(status, objectMapper.writeValueAsString(analytics));
                    } catch (Exception e) {
                        log.error("Error serializing analytics", e);
                        return KeyValue.pair(status, "{}");
                    }
                })
                .to("order.analytics", Produced.with(Serdes.String(), Serdes.String()));
    }

    private void detectHighValueOrders(KStream<String, String> orderCreatedStream) {
        orderCreatedStream
                .mapValues(this::parseJson)
                .filter((key, order) -> {
                    if (order != null && order.has("totalAmount")) {
                        double amount = order.get("totalAmount").asDouble();
                        return amount > 1000.0;
                    }
                    return false;
                })
                .peek((key, order) -> {
                    String orderId = order.has("orderId") ? order.get("orderId").asText() : "unknown";
                    double amount = order.has("totalAmount") ? order.get("totalAmount").asDouble() : 0;
                    String customer = order.has("customerName") ? order.get("customerName").asText() : "unknown";

                    log.warn("🚨 HIGH VALUE ORDER: orderId={}, amount={}, customer={}",
                            orderId, amount, customer);
                })
                .mapValues(order -> {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("type", "HIGH_VALUE_ORDER");
                    alert.put("orderId", order.has("orderId") ? order.get("orderId").asText() : "unknown");
                    alert.put("amount", order.has("totalAmount") ? order.get("totalAmount").asDouble() : 0);
                    alert.put("customer", order.has("customerName") ? order.get("customerName").asText() : "unknown");
                    alert.put("customerId", order.has("customerId") ? order.get("customerId").asText() : "unknown");

                    try {
                        return objectMapper.writeValueAsString(alert);
                    } catch (Exception e) {
                        log.error("Error serializing alert", e);
                        return "{}";
                    }
                })
                .to("order.analytics", Produced.with(Serdes.String(), Serdes.String()));
    }

    private void calculateTotalRevenue(KStream<String, String> orderCreatedStream) {
        orderCreatedStream
                .mapValues(this::parseJson)
                .filter((key, order) -> order != null && order.has("totalAmount"))
                .groupBy(
                        (key, order) -> "TOTAL_REVENUE",
                        Grouped.with(Serdes.String(), new JsonSerde<>(JsonNode.class, objectMapper))
                )
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(10)))
                .aggregate(
                        () -> 0.0,
                        (key, orderJson, aggregate) -> {
                            double amount = orderJson.get("totalAmount").asDouble();
                            return aggregate + amount;
                        },
                        Materialized.with(Serdes.String(), Serdes.Double())
                )
                .toStream()
                .peek((windowedKey, totalRevenue) -> {
                    log.info("💰 Revenue Analytics: total={}, window=[{} - {}]",
                            totalRevenue,
                            windowedKey.window().startTime(),
                            windowedKey.window().endTime());
                })
                .map((windowedKey, totalRevenue) -> {
                    Map<String, Object> analytics = new HashMap<>();
                    analytics.put("type", "TOTAL_REVENUE");
                    analytics.put("amount", totalRevenue);
                    analytics.put("windowStart", windowedKey.window().startTime().toString());
                    analytics.put("windowEnd", windowedKey.window().endTime().toString());

                    try {
                        return KeyValue.pair("REVENUE", objectMapper.writeValueAsString(analytics));
                    } catch (Exception e) {
                        log.error("Error serializing revenue analytics", e);
                        return KeyValue.pair("REVENUE", "{}");
                    }
                })
                .to("order.analytics", Produced.with(Serdes.String(), Serdes.String()));
    }

    private void countOrdersByCustomer(KStream<String, String> orderCreatedStream) {
        orderCreatedStream
                .mapValues(this::parseJson)
                .filter((key, order) -> order != null && order.has("customerId"))
                .groupBy(
                        (key, order) -> order.get("customerId").asText(),
                        Grouped.with(Serdes.String(), new JsonSerde<>(JsonNode.class, objectMapper))
                )
                .count(Materialized.as("orders-by-customer-store"))
                .toStream()
                .filter((customerId, count) -> count >= 3)
                .peek((customerId, count) -> {
                    log.info("👤 Customer Analytics: customerId={}, totalOrders={}",
                            customerId, count);
                })
                .foreach((customerId, count) -> {
                    log.info("⭐ LOYAL CUSTOMER: customerId={}, orderCount={}", customerId, count);
                });
    }

    private void analyzePopularProducts(KStream<String, String> orderCreatedStream) {
        orderCreatedStream
                .mapValues(this::parseJson)
                .filter((key, order) -> order != null && order.has("items"))
                .flatMapValues(order -> {
                    var items = new java.util.ArrayList<String>();
                    if (order.has("items") && order.get("items").isArray()) {
                        order.get("items").forEach(item -> {
                            if (item.has("productId")) {
                                items.add(item.get("productId").asText());
                            }
                        });
                    }
                    return items;
                })
                .groupBy(
                        (key, productId) -> productId,
                        Grouped.with(Serdes.String(), Serdes.String())
                )
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(15)))
                .count(Materialized.as("product-popularity-store"))
                .toStream()
                .filter((windowedKey, count) -> count >= 3)
                .peek((windowedKey, count) -> {
                    log.info("🔥 POPULAR PRODUCT: productId={}, orders={}, window=[{} - {}]",
                            windowedKey.key(),
                            count,
                            windowedKey.window().startTime(),
                            windowedKey.window().endTime());
                });
    }

    private JsonNode parseJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception e) {
            log.error("Error parsing JSON: {}", e.getMessage());
            return null;
        }
    }
}
