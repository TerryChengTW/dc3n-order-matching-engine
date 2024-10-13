package com.matching.repository;

import com.matching.model.MarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, String> {
    @Query("SELECT md FROM MarketData md WHERE md.symbol = :symbol AND md.timestamp = " +
            "(SELECT MAX(m.timestamp) FROM MarketData m WHERE m.symbol = :symbol AND m.timestamp < :time)")
    MarketData findLatestBeforeTime(String symbol, Instant time);

    List<MarketData> findTop500BySymbolAndTimeFrameOrderByTimestampDesc(String symbol, String timeFrame);

    List<MarketData> findTop2BySymbolOrderByTimestampDesc(String symbol);

    // 查詢特定時間範圍內的1分鐘K線數據
    List<MarketData> findBySymbolAndTimeFrameAndTimestampBetween(String symbol, String timeFrame, Instant startTime, Instant endTime);

    // 查詢1分鐘K線數據
    @Query("SELECT md FROM MarketData md WHERE md.symbol = :symbol AND md.timeFrame = '1m' AND md.timestamp < :timestamp ORDER BY md.timestamp DESC LIMIT 500")
    List<MarketData> findTop500BySymbolAnd1mTimeFrameBeforeOrderByTimestampDesc(String symbol, Instant timestamp);

    // 查詢5分鐘K線數據
    @Query("SELECT md FROM MarketData md WHERE md.symbol = :symbol AND md.timeFrame = '5m' AND md.timestamp < :timestamp ORDER BY md.timestamp DESC LIMIT 500")
    List<MarketData> findTop500BySymbolAnd5mTimeFrameBeforeOrderByTimestampDesc(String symbol, Instant timestamp);

    // 查詢1小時K線數據
    @Query("SELECT md FROM MarketData md WHERE md.symbol = :symbol AND md.timeFrame = '1h' AND md.timestamp < :timestamp ORDER BY md.timestamp DESC LIMIT 500")
    List<MarketData> findTop500BySymbolAnd1hTimeFrameBeforeOrderByTimestampDesc(String symbol, Instant timestamp);
}
