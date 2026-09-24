package com.example.JustBuyIt.Services;

import com.example.JustBuyIt.Models.Role;
import com.example.JustBuyIt.Models.Users;
import com.example.JustBuyIt.Repository.UsersRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsersService {

    private final UsersRepo usersRepo;

    private final RedisCacheService redisCacheService;

    public UsersService(UsersRepo usersRepo, RedisCacheService redisCacheService) {
        this.usersRepo = usersRepo;
        this.redisCacheService = redisCacheService;
    }

    List<Users> getUsers() {
        return usersRepo.findAll();
    }

    Users getUser(Long id) {
        return usersRepo.findById(id).orElseThrow(() -> new RuntimeException("User Not Found!"));
    }

    public Users getUserByEmail(String name) {
        return (Users) usersRepo.findByEmail(name).orElseThrow(() -> new RuntimeException("User Not Found!"));
    }

    public List<Users> getAllAdmins() {
        return usersRepo.findAllAdmins();
    }

    public Page<Users> getUsersPaged(String q, Pageable pageable) {
        if (q != null && !q.isBlank()) {
            return usersRepo.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(q, q, pageable);
        }
        return usersRepo.findAllRegularUsers(pageable);
    }

    public Users setBanned(Long id, boolean banned) {
        Users u = usersRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User Not Found!"));
        if (u.getRole() == Role.ADMIN) {
            throw new IllegalStateException("Admins cannot be banned");
        }
        u.setBanned(banned);
        Users saved = usersRepo.save(u);
        redisCacheService.setCacheUser(saved.getEmail(), saved);
        return saved;
    }
}
