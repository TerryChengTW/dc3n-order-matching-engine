package com.matching.service;

import com.matching.dto.TradeOrdersMessage;
import com.matching.model.Order;
import com.matching.model.Trade;
import com.matching.producer.MatchedOrderProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisCallback;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class NewOrderbookService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final String BUY_SUFFIX = ":BUY";
    private final String SELL_SUFFIX = ":SELL";
    private MatchedOrderProducer matchedOrderProducer;

    public NewOrderbookService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper, MatchedOrderProducer matchedOrderProducer) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.matchedOrderProducer = matchedOrderProducer;
    }

    // 獲取對手方最佳訂單
    public Order getBestOpponentOrder(Order newOrder) {
        // 決定對手方的 key
        String opponentKey = newOrder.getSymbol() + (newOrder.getSide() == Order.Side.BUY ? SELL_SUFFIX : BUY_SUFFIX);

        Set<TypedTuple<Object>> opponentOrders;

        // 根據 `side` 選擇 ZSet 排序方式
        if (newOrder.getSide() == Order.Side.BUY) {
            // 如果 `newOrder` 是買單，取 `score` 最小的賣單
            opponentOrders = redisTemplate.opsForZSet().rangeWithScores(opponentKey, 0, 0);
        } else {
            // 如果 `newOrder` 是賣單，取 `score` 最大的買單
            opponentOrders = redisTemplate.opsForZSet().reverseRangeWithScores(opponentKey, 0, 0);
        }

        // 解析對手訂單
        if (opponentOrders != null && !opponentOrders.isEmpty()) {
            TypedTuple<Object> bestOpponent = opponentOrders.iterator().next();
            return parseOrderFromJson((String) bestOpponent.getValue());
        }

        return null;
    }

    // 解析 JSON 字符串到 Order 對象
    private Order parseOrderFromJson(String orderJson) {
        try {
            return objectMapper.readValue(orderJson, Order.class);
        } catch (JsonProcessingException e) {
            System.err.println("Error: Failed to parse JSON. Order JSON: " + orderJson);
            e.printStackTrace();
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    // 儲存訂單到 Redis
    public void saveOrderToRedis(Order order) {
        final byte[] redisKey = getRedisKeyBytes(order.getSymbol(), order.getSide());
        final double score = calculateScore(order);
        final byte[] jsonBytes = convertOrderToBytes(order);

        // 使用 ZSet 儲存完整的訂單 JSON
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            connection.zSetCommands().zAdd(redisKey, score, jsonBytes);
            return null;
        });
    }

    // 計算 ZSet 的 score
    private double calculateScore(Order order) {
        int precision = 7;
        BigDecimal precisionFactor = BigDecimal.TEN.pow(precision);
        Instant modifiedTime = order.getModifiedAt();
        BigDecimal calculatedScore = order.getPrice().multiply(precisionFactor)
                .add((order.getSide() == Order.Side.BUY ? BigDecimal.valueOf(-1) : BigDecimal.ONE).multiply(BigDecimal.valueOf(modifiedTime.toEpochMilli())));
        return calculatedScore.doubleValue();
    }

    // 從 Redis 中移除訂單，使用原始的 JSON 值
    public void removeOrderFromRedis(Order order, String originalJson) {
        byte[] redisKey = getRedisKeyBytes(order.getSymbol(), order.getSide());
        byte[] originalBytes = originalJson.getBytes(StandardCharsets.UTF_8);

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            connection.zSetCommands().zRem(redisKey, originalBytes);
            return null;
        });
    }

    // 更新 Redis 中的訂單，先刪除舊的再更新
    public void updateOrderInRedis(Order order, String originalJson) {
        byte[] redisKey = getRedisKeyBytes(order.getSymbol(), order.getSide());
        byte[] originalBytes = originalJson.getBytes(StandardCharsets.UTF_8);
        double newScore = calculateScore(order);
        byte[] updatedBytes = convertOrderToBytes(order);

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            // 移除舊的訂單
            connection.zSetCommands().zRem(redisKey, originalBytes);
            // 插入新的訂單
            connection.zSetCommands().zAdd(redisKey, newScore, updatedBytes);
            return null;
        });
    }

    // 轉換訂單為 JSON 字符串
    public String convertOrderToJson(Order order) {
        try {
            return objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            System.err.println("Error: Failed to convert order to JSON. Order: " + order);
            e.printStackTrace();
            throw new RuntimeException("Failed to convert order to JSON", e);
        }
    }


    // 獲取 Redis key 的字節數組
    private byte[] getRedisKeyBytes(String symbol, Order.Side side) {
        return (symbol + (side == Order.Side.BUY ? BUY_SUFFIX : SELL_SUFFIX)).getBytes(StandardCharsets.UTF_8);
    }

    // 轉換訂單為字節數組
    private byte[] convertOrderToBytes(Order order) {
        try {
            return objectMapper.writeValueAsBytes(order);
        } catch (JsonProcessingException e) {
            System.err.println("Error: Failed to convert order to JSON bytes. Order: " + order);
            e.printStackTrace();
            throw new RuntimeException("Failed to convert order to JSON bytes", e);
        }
    }


    public void saveAllOrdersAndTrades(List<Trade> trades) {
        // 將訂單和交易信息封裝到 MatchedMessage 中並發送到 Kafka
        for (Trade trade : trades) {
            Order buyOrder = trade.getBuyOrder();
            Order sellOrder = trade.getSellOrder();

            TradeOrdersMessage tradeOrdersMessage = new TradeOrdersMessage(buyOrder, sellOrder, trade);

            // 發送到 Kafka，type 為 "TRADE_ORDER"
            matchedOrderProducer.sendMatchedTrade(tradeOrdersMessage);
        }
    }

}
