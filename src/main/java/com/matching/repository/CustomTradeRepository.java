package com.matching.repository;

import com.matching.model.Order;
import com.matching.model.Trade;

import java.util.List;

public interface CustomTradeRepository {
    void saveAllOrdersAndTrades(List<Order> buyOrders, List<Order> sellOrders, List<Trade> trades);
}
