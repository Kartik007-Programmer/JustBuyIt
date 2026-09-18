package com.example.JustBuyIt.DTOs;

public class CartItemResponse {
    private Long id;
    private Integer productId;
    private String productName;
    private Double unitPrice;
    private Integer quantity;
    private Double subTotal;

    public CartItemResponse() {}

    public CartItemResponse(Long id, Integer productId, String productName, Double unitPrice, Integer quantity, Double subTotal) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subTotal = subTotal;
    }

    public CartItemResponse(String name, Integer quantity, Double price, double v) {
        this.productName = name;
        this.unitPrice = price;
        this.quantity = quantity;
        this.subTotal = v;
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getSubTotal() { return subTotal; }
    public void setSubTotal(Double subTotal) { this.subTotal = subTotal; }

}
