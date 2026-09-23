package com.example.JustBuyIt.Controllers;

import com.example.JustBuyIt.Models.Users;
import com.example.JustBuyIt.Services.SecurityService;
import com.example.JustBuyIt.Services.UsersService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        Users admin = securityService.getPresentAuthorizedAdmin();
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
}
