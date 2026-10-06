package com.example.JustBuyIt.Controllers;

import com.example.JustBuyIt.DTOs.UserPrincipalDto;
import com.example.JustBuyIt.Models.Role;
import com.example.JustBuyIt.Models.Users;
import com.example.JustBuyIt.Services.SecurityService;
import com.example.JustBuyIt.Services.UsersService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final SecurityService securityService;
    private final UsersService usersService;

    public AdminController(SecurityService securityService, UsersService usersService) {
        this.securityService = securityService;
        this.usersService = usersService;
    }

    @GetMapping
    ResponseEntity<?> getCurrentAdmin() {
        UserPrincipalDto admin = securityService.getPresentAuthorizedAdmin();
        if (admin != null) {
            return ResponseEntity.ok(admin);
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/all")
    ResponseEntity<List<Users>> getAllAdmins() {
        return ResponseEntity.ok(usersService.getAllAdmins());
    }

    @GetMapping("/users")
    ResponseEntity<Page<Users>> listUsers(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(usersService.getUsersPaged(q, pageable));
    }

    @PatchMapping("/users/{id}/ban")
    ResponseEntity<?> banUser(@PathVariable Long id) {
        UserPrincipalDto actor = securityService.getPresentAuthorizedUser();
        if (actor.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only full admins can ban users.");
        }
        try {
            return ResponseEntity.ok(usersService.setBanned(id, true));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/users/{id}/unban")
    ResponseEntity<?> unbanUser(@PathVariable Long id) {
        UserPrincipalDto actor = securityService.getPresentAuthorizedUser();
        if (actor.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only full admins can ban users.");
        }
        try {
            return ResponseEntity.ok(usersService.setBanned(id, false));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
