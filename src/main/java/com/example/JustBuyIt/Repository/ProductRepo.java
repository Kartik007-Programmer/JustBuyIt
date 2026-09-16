package com.example.JustBuyIt.Repository;

import com.example.JustBuyIt.Models.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepo extends JpaRepository<Product,Integer>{
    @Query("SELECT p FROM Product p WHERE "+
            "LOWER(p.name) LIKE LOWER(CONCAT('%',:keyword,'%')) OR "+
            "LOWER(p.description) LIKE LOWER(CONCAT('%',:keyword,'%')) ")
    List<Product> searchProductByKeyword(String keyword);

    @Query("SELECT p FROM Product p WHERE "+
            "LOWER(p.name) LIKE LOWER(CONCAT('%',:keyword,'%')) OR "+
            "LOWER(p.description) LIKE LOWER(CONCAT('%',:keyword,'%')) ")
    Page<Product> searchProductByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Page<Product> findByCategory_NameIgnoreCase(String categoryName, Pageable pageable);

    @Query("""
       SELECT p FROM Product p
       WHERE (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
       LOWER(p.description) LIKE LOWER(CONCAT('%',:keyword,'%')))
         AND (:category IS NULL OR LOWER(p.category.name) = LOWER(:category))
       """)
    Page<Product> searchByKeywordAndCategory(@Param("keyword") String keyword,
                                             @Param("category") String category,
                                             Pageable pageable);
}
