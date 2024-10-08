package com.matching.dto;

import com.matching.model.Order;
import com.matching.model.Trade;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class  TradeOrdersMessage {
    // Getters and Setters
    private Order buyOrder;
    private Order sellOrder;
    private Trade trade;

}
