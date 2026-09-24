package com.example.JustBuyIt.Repository;

import com.example.JustBuyIt.Models.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsersRepo extends JpaRepository<Users,Long> {

    Optional<Users> findByEmail(String email);

    Optional<Users> findByVerificationToken(String token);

    Optional<Users> findByPasswordResetToken(String token);

    @Query("SELECT a FROM Users a WHERE a.role = 'ADMIN'")
    List<Users> findAllAdmins();

    @Query("SELECT u FROM Users u WHERE u.role = 'USER'")
    Page<Users> findAllRegularUsers(Pageable pageable);

    Page<Users> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name, String email, Pageable pageable);
}
