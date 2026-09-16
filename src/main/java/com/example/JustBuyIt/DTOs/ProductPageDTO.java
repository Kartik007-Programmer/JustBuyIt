package com.example.JustBuyIt.DTOs;

import com.example.JustBuyIt.Models.Product;
import java.io.Serializable;
import java.util.List;

public class ProductPageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Product> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public ProductPageDTO() {}   // no-arg ctor for Jackson

    public ProductPageDTO(List<Product> content, int page, int size,
                          long totalElements, int totalPages,
                          boolean first, boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
    }

    // getters and setters for all fields
    public List<Product> getContent() { return content; }
    public void setContent(List<Product> content) { this.content = content; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public boolean isFirst() { return first; }
    public void setFirst(boolean first) { this.first = first; }
    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }
}