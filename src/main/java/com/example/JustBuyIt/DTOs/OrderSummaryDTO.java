package com.example.JustBuyIt.DTOs;

import java.time.LocalDateTime;

public class OrderSummaryDTO {
    private Long id;
    private String transactionId;
    private Double totalAmount;
    private String paymentMethod;
    private String status;
    private LocalDateTime placedAt;
    private Integer itemCount;

    public OrderSummaryDTO() {}

    public OrderSummaryDTO(Long id, String transactionId, Double totalAmount,
                           String paymentMethod, String status,
                           LocalDateTime placedAt, Integer itemCount) {
        this.id = id;
        this.transactionId = transactionId;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.placedAt = placedAt;
        this.itemCount = itemCount;
    }

    /* getters + setters ... */
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getPlacedAt() { return placedAt; }
    public void setPlacedAt(LocalDateTime placedAt) { this.placedAt = placedAt; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
}