package com.example.JustBuyIt.DTOs;

public class ProductDTO {
    private String name;
    private Double price;
    private String description;
    private Integer quantity;
    private String qunatityUnit;
    private String categoryName;
    private String imageUrl;     // Used for JSON URL batch imports

    public ProductDTO() {}

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getQunatityUnit() { return qunatityUnit; }
    public void setQunatityUnit(String qunatityUnit) { this.qunatityUnit = qunatityUnit; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
