package com.example.JustBuyIt.Repository;

import com.example.JustBuyIt.Models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepo extends JpaRepository<Product,Integer>{
//
    @Query("SELECT p FROM Product p WHERE "+
            "LOWER(p.name) LIKE LOWER(CONCAT('%',:keyword,'%')) OR "+
            "LOWER(p.description) LIKE LOWER(CONCAT('%',:keyword,'%')) ")
    List<Product> searchProductByKeyword(String keyword);
}
