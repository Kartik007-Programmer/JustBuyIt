package com.example.JustBuyIt.Repository;

import com.example.JustBuyIt.Models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepo extends JpaRepository<Category, Integer> {

    Optional<Category> findByName(String categoryName);

    @Query("SELECT DISTINCT c.name FROM Category c ORDER BY c.name ASC")
    List<String> findAllName();

}
