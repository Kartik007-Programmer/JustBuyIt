package com.example.JustBuyIt.Repository;

import com.example.JustBuyIt.Models.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepo extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.placedAt DESC")
    List<Order> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.placedAt DESC")
    Page<Order> findPageByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.user.id = :userId")
    Optional<Order> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}