package com.matching.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "market_data")
@IdClass(MarketDataId.class)
@Data
@NoArgsConstructor
public class MarketData {

    @Id
    @Column(length = 10, nullable = false)
    private String symbol;

    @Id
    @Column(length = 5, nullable = false)
    private String timeFrame; // 如 '1m', '5m', '1d'

    @Id
    private Instant timestamp;

    @Column(precision = 18, scale = 8, nullable = false)
    private BigDecimal open;

    @Column(precision = 18, scale = 8, nullable = false)
    private BigDecimal high;

    @Column(precision = 18, scale = 8, nullable = false)
    private BigDecimal low;

    @Column(precision = 18, scale = 8, nullable = false)
    private BigDecimal close;

    @Column(precision = 18, scale = 8, nullable = false)
    private BigDecimal volume;
}
