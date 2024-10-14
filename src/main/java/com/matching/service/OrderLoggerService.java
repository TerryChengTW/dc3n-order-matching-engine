package com.matching.service;

import com.matching.model.Order;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderLoggerService {

    private static final String FILE_PATH = "order_logs.xlsx";
    private final List<OrderLogEntry> orderLogs = new ArrayList<>(); // 暫存區
    private final Object lock = new Object(); // 用於線程安全

    // 定義訂單記錄的結構
    public static class OrderLogEntry {
        private final String orderId;
        private final String userId;
        private final String symbol;
        private final String price;
        private final String quantity;
        private final String side;
        private final long completionTime;  // 使用 Unix 時間戳（毫秒）

        public OrderLogEntry(Order order, Instant completionTime) {
            this.orderId = order.getId();
            this.userId = order.getUserId();
            this.symbol = order.getSymbol();
            this.price = order.getPrice().toString();
            this.quantity = order.getQuantity().toString();
            this.side = order.getSide().toString();
            this.completionTime = completionTime.toEpochMilli();  // 記錄 Unix 時間戳
        }

        // Getters
        public String getOrderId() { return orderId; }
        public String getUserId() { return userId; }
        public String getSymbol() { return symbol; }
        public String getPrice() { return price; }
        public String getQuantity() { return quantity; }
        public String getSide() { return side; }
        public long getCompletionTime() { return completionTime; }  // 以毫秒為單位的時間戳
    }

    // 每次接收訂單時記錄到暫存區
    public void logOrderCompletion(Order order, Instant completionTime) {
        synchronized (lock) {
            orderLogs.add(new OrderLogEntry(order, completionTime)); // 將記錄追加到暫存區
        }
    }

    // 每30秒將暫存的資料批量寫入到 Excel 中
    @Scheduled(fixedRate = 30000)
    public void writeOrdersToExcel() {
        synchronized (lock) {
            if (!orderLogs.isEmpty()) {
                File file = new File(FILE_PATH);
                Workbook workbook = null;
                FileOutputStream fileOut = null;

                try {
                    // 檢查檔案是否存在
                    if (file.exists()) {
                        FileInputStream fileInputStream = new FileInputStream(file);
                        workbook = new XSSFWorkbook(fileInputStream); // 打開現有檔案
                    } else {
                        workbook = new XSSFWorkbook(); // 創建新檔案
                    }

                    // 檢查是否已經存在工作表，否則創建一個新的工作表
                    Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : workbook.createSheet("Orders");

                    // 如果是新檔案或第一次寫入，創建標題列
                    if (sheet.getLastRowNum() == 0) {
                        Row header = sheet.createRow(0);
                        header.createCell(0).setCellValue("Order ID");
                        header.createCell(1).setCellValue("User ID");
                        header.createCell(2).setCellValue("Symbol");
                        header.createCell(3).setCellValue("Price");
                        header.createCell(4).setCellValue("Quantity");
                        header.createCell(5).setCellValue("Side");
                        header.createCell(6).setCellValue("Completion Time (Unix Timestamp)");
                    }

                    // 找到目前已經有的最後一行的行號
                    int rowIndex = sheet.getLastRowNum() + 1;

                    // 將資料填入Excel
                    for (OrderLogEntry log : orderLogs) {
                        Row row = sheet.createRow(rowIndex++);
                        row.createCell(0).setCellValue(log.getOrderId());
                        row.createCell(1).setCellValue(log.getUserId());
                        row.createCell(2).setCellValue(log.getSymbol());
                        row.createCell(3).setCellValue(log.getPrice());
                        row.createCell(4).setCellValue(log.getQuantity());
                        row.createCell(5).setCellValue(log.getSide());
                        row.createCell(6).setCellValue(log.getCompletionTime());  // 使用 Unix 時間戳
                    }

                    // 寫入到檔案
                    fileOut = new FileOutputStream(FILE_PATH);
                    workbook.write(fileOut);

                    // 清空暫存區
                    orderLogs.clear();

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (workbook != null) {
                            workbook.close();
                        }
                        if (fileOut != null) {
                            fileOut.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
