package com.matching.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service  // 註冊為 Spring 的服務
public class KlineScheduler {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Scheduled(cron = "0 * * * * *")  // 每分鐘0秒執行
    public void sendEmptyKlineUpdate() throws JsonProcessingException {
        // 構建 BTCUSDT 的空K線消息
        String btcusdtKlineMessage = objectMapper.writeValueAsString(Map.of(
                "symbol", "BTCUSDT",
                "price", "-1",  // 使用 -1 作為空K棒的標識
                "tradeTime", Instant.now().getEpochSecond()
        ));

        // 構建 ETHUSDT 的空K線消息
        String ethusdtKlineMessage = objectMapper.writeValueAsString(Map.of(
                "symbol", "ETHUSDT",
                "price", "-1",  // 使用 -1 作為空K棒的標識
                "tradeTime", Instant.now().getEpochSecond()
        ));
        System.out.println("Sending empty kline updates: " + btcusdtKlineMessage);

        // 發送 BTCUSDT 和 ETHUSDT 的空K線更新數據到 Kafka
        kafkaTemplate.send("kline-updates", btcusdtKlineMessage);
        kafkaTemplate.send("kline-updates", ethusdtKlineMessage);
    }
}
