package com.example.JustBuyIt.DTOs;

import java.util.List;

public class ShoppingCartResponse {
    private Long cartId;
    private List<CartItemResponse> items;
    private Double totalPrice;

    public ShoppingCartResponse() {}

    public ShoppingCartResponse(Long cartId, List<CartItemResponse> items, Double totalPrice) {
        this.cartId = cartId;
        this.items = items;
        this.totalPrice = totalPrice;
    }

    public Long getCartId() { return cartId; }
    public void setCartId(Long cartId) { this.cartId = cartId; }

    public List<CartItemResponse> getItems() { return items; }
    public void setItems(List<CartItemResponse> items) { this.items = items; }

    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }

}
