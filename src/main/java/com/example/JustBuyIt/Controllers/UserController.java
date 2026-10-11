package com.example.JustBuyIt.Controllers;

import com.example.JustBuyIt.DTOs.OrderDetailDTO;
import com.example.JustBuyIt.DTOs.OrderSummaryDTO;
import com.example.JustBuyIt.DTOs.ProfileUpdateRequest;
import com.example.JustBuyIt.DTOs.UserPrincipalDto;
import com.example.JustBuyIt.Models.Users;
import com.example.JustBuyIt.Repository.UsersRepo;
import com.example.JustBuyIt.Services.OrderService;
import com.example.JustBuyIt.Services.SecurityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final SecurityService securityService;
    private final OrderService orderService;
    private final UsersRepo usersRepo;

    public UserController(SecurityService securityService, OrderService orderService, UsersRepo usersRepo) {
        this.securityService = securityService;
        this.orderService = orderService;
        this.usersRepo = usersRepo;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            UserPrincipalDto principal = securityService.getPresentAuthorizedUser();
            Users user = usersRepo.findByEmail(principal.getEmail()).orElseThrow();
            return ResponseEntity.ok(Map.of(
                    "id",    user.getId(),
                    "name",  user.getName() != null ? user.getName() : "",
                    "email", user.getEmail(),
                    "role",  user.getRole().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }

    // This is Users Order methods
    @GetMapping("/orders")
    public ResponseEntity<List<OrderSummaryDTO>> listOrders() {
        String email = securityService.getPresentAuthorizedUser().getEmail();
        return ResponseEntity.ok(orderService.getOrdersFor(email));
    }

    @GetMapping("/orders/paged")
    public ResponseEntity<Page<OrderSummaryDTO>> listPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String email = securityService.getPresentAuthorizedUser().getEmail();
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(orderService.getOrdersFor(email, pageable));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderDetailDTO> getOrder(@PathVariable Long id) {
        String email = securityService.getPresentAuthorizedUser().getEmail();
        return ResponseEntity.ok(orderService.getOrderDetail(email, id));
    }

    //This is Users Profile methods
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        String email = securityService.getPresentAuthorizedUser().getEmail();
        Users user = usersRepo.findByEmail(email).orElseThrow();

        return ResponseEntity.ok(Map.of(
                "id",      user.getId(),
                "name",    user.getName() != null ? user.getName() : "",
                "email",   user.getEmail(),
                "phone",   user.getPhone() != null ? user.getPhone() : "",
                "address", user.getAddress() != null ? user.getAddress() : "",
                "role",    user.getRole().name()
        ));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileUpdateRequest req) {
        String email = securityService.getPresentAuthorizedUser().getEmail();
        Users user = usersRepo.findByEmail(email).orElseThrow();

        if (req.getName() != null && !req.getName().isBlank()) {
            user.setName(req.getName().trim());
        }
        user.setPhone(req.getPhone());
        user.setAddress(req.getAddress());
        usersRepo.save(user);

        return ResponseEntity.ok(Map.of("message", "Profile updated"));
    }
}
