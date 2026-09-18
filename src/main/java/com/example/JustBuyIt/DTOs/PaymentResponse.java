package com.example.JustBuyIt.DTOs;

import java.util.List;

public class PaymentResponse {
    private String transactionId;
    private Double amount;
    private String currency = "INR";
    private String status;
    private String message;
    private String paymentMethod;
    private List<CartItemResponse> items;   // 🆕 snapshot of the order

    public PaymentResponse() {
    }

    public PaymentResponse(String transactionId, Double amount, String status, String message, String paymentMethod, List<CartItemResponse> items) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.status = status;
        this.message = message;
        this.paymentMethod = paymentMethod;
        this.items = items;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public List<CartItemResponse> getItems() {
        return items;
    }

    public void setItems(List<CartItemResponse> items) {
        this.items = items;
    }
}
