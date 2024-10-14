package com.matching.consumer;

import com.matching.model.Order;
import com.matching.service.NewOrderMatchingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderConsumer {

    private final ObjectMapper objectMapper;
    private final NewOrderMatchingService newMatchingService;

    public OrderConsumer(
            ObjectMapper objectMapper,
            @Lazy NewOrderMatchingService newMatchingService) {
        this.objectMapper = objectMapper;
        this.newMatchingService = newMatchingService;
    }

    // 批量消費新訂單
    @KafkaListener(topics = "new_orders", groupId = "order_group", containerFactory = "batchFactory")
    public void consumeNewOrders(List<String> orderJsonList) {
        for (String orderJson : orderJsonList) {
            try {
                // 將每筆 JSON 訂單轉換為 Order 對象
                Order order = objectMapper.readValue(orderJson, Order.class);
                newMatchingService.handleNewOrder(order);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
