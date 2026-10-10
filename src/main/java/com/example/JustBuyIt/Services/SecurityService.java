package com.example.JustBuyIt.Services;

import com.example.JustBuyIt.DTOs.UserPrincipalDto;
import com.example.JustBuyIt.Models.Product;
import com.example.JustBuyIt.Models.Role;
import com.example.JustBuyIt.Models.ShoppingCart;
import com.example.JustBuyIt.Models.Users;
import com.example.JustBuyIt.Repository.ProductRepo;
import com.example.JustBuyIt.Repository.ShoppingCartRepo;
import com.example.JustBuyIt.Repository.UsersRepo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@Service
public class SecurityService {

    private final JwtService jwtService;
    private final UsersRepo usersRepo;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final RedisCacheService redisCacheService;
    private final EmailService  emailService;
    private final ShoppingCartRepo cartRepo;
    private final ProductRepo productRepo;
    public static final int SECONDARY_ADMIN_PRODUCT_LIMIT = 3;

    public SecurityService(JwtService jwtService, UsersRepo usersRepo, AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder, UserDetailsService userDetailsService, RedisCacheService redisCacheService, EmailService emailService, ShoppingCartRepo cartRepo, ProductRepo productRepo) {
        this.jwtService = jwtService;
        this.usersRepo = usersRepo;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.redisCacheService = redisCacheService;
        this.emailService = emailService;
        this.cartRepo = cartRepo;
        this.productRepo = productRepo;
    }

    public ResponseEntity<?> RegisterUser(Users users) {

        // Set default role if not provided
        if (users.getRole() == null) {
            users.setRole(Role.USER);
        }

        // Prevent duplicate email
        if (usersRepo.findByEmail(users.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already registered"));
        }

        users.setPassword(passwordEncoder.encode(users.getPassword()));
        users.setEmailVerified(false);

        String token = UUID.randomUUID().toString();
        users.setVerificationToken(token);
        users.setVerificationTokenExpiry(LocalDateTime.now().plusMinutes(1440));

        Users savedUser = usersRepo.save(users);
        ShoppingCart cart = new ShoppingCart();
        cart.setUser(savedUser);
        cartRepo.save(cart);

        // Cache the new user
        redisCacheService.setCacheUser(savedUser.getEmail(), UserPrincipalDto.fromEntity(savedUser));

        try {
            emailService.sendVerificationEmail(savedUser.getEmail(), token, savedUser.getName());
        } catch (Exception e) {
            // don't fail registration if mail is down
            System.err.println("Failed to send verification email: " + e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    public ResponseEntity<?> verifyEmail(String token) {
        Optional<Users> opt = usersRepo.findByVerificationToken(token);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid verification token");
        }
        Users user = opt.get();
        if (user.getVerificationTokenExpiry() == null
                || user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Verification token expired");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        usersRepo.save(user);
        redisCacheService.setCacheUser(user.getEmail(), UserPrincipalDto.fromEntity(user));

        return ResponseEntity.ok("Email verified successfully");
    }

    public ResponseEntity<?> requestPasswordReset(String email) {
        Optional<Users> opt = usersRepo.findByEmail(email);
        // Always return OK to avoid user enumeration
        if (opt.isPresent()) {
            Users user = opt.get();
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
            usersRepo.save(user);
            redisCacheService.setCacheUser(user.getEmail(), UserPrincipalDto.fromEntity(user));

            try {
                emailService.sendPasswordResetEmail(user.getEmail(), token, user.getName());
            } catch (Exception e) {
                System.err.println("Failed to send reset email: " + e.getMessage());
            }
        }
        return ResponseEntity.ok("If that email exists, a reset link has been sent.");
    }

    public ResponseEntity<?> resetPassword(String token, String newPassword) {
        Optional<Users> opt = usersRepo.findByPasswordResetToken(token);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid reset token");
        }
        Users user = opt.get();
        if (user.getPasswordResetTokenExpiry() == null
                || user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Reset token expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now());
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        usersRepo.save(user);
        redisCacheService.setCacheUser(user.getEmail(), UserPrincipalDto.fromEntity(user));

        return ResponseEntity.ok("Password reset successful");
    }

    public ResponseEntity<?> VerifyUserByUsernamePassword(String username, String password, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(username, password));
            if (authentication.isAuthenticated()) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                String token = jwtService.generateToken(userDetails);

//                System.out.println("token: " + token);

                // Cache the user and token in Redis
                redisCacheService.setCacheUser(username, UserPrincipalDto.fromEntity((Users) userDetails));
                redisCacheService.setCacheToken(token,username);

                ResponseCookie responseCookie = ResponseCookie.from("JWT_TOKEN",token)
                        .httpOnly(true)
                        .secure(false)
                        .path("/")
                        .maxAge(60 * 60)
                        .sameSite("Lax")
                        .build();
                response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Please verify your email before signing in.");
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password.");
        } catch (LockedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Your account has been suspended. Contact support.");
        }
    }

    public ResponseEntity<?> Logout(HttpServletRequest request, HttpServletResponse response) {
        // Get current authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            // Invalidate cache
            redisCacheService.invalidateUserCache(userDetails.getUsername());
        }

        // 1. Try resolving token from Cookie
        String token = null;
        Cookie cookie = WebUtils.getCookie(request, "JWT_TOKEN");
        if (cookie != null) {
            token = cookie.getValue();
        }

        // 2. Fallback or override using Authorization Header
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        // Invalidate whichever token was provided
        if (token != null) {
            redisCacheService.invalidateTokenCache(token);
        }

        ResponseCookie deleteCookie = ResponseCookie.from("JWT_TOKEN", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        return ResponseEntity.ok("Logged out successfully");
    }

    public String getPresentAuthorizedRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "GUEST";
        }
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority())
                .orElse("USER");
    }

    public UserPrincipalDto getPresentAuthorizedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user");
        }
        String username = authentication.getName();
        UserPrincipalDto user = null;

        UserPrincipalDto Cachedusers = redisCacheService.getUserFromCache(username);

        if (Cachedusers != null) {
            user = Cachedusers;
        }else {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            if (userDetails instanceof Users) {
                user = UserPrincipalDto.fromEntity((Users) userDetails);
                redisCacheService.setCacheUser(username, user);
            }
        }
        return user;
    }

    public UserPrincipalDto getPresentAuthorizedAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipalDto user = null;
        assert authentication != null;
        String username = authentication.getName();
        String role = getPresentAuthorizedRole();
        if (role.equals("ADMIN") || role.equals("SECONDARY_ADMIN")) {
            UserPrincipalDto Cachedusers = redisCacheService.getUserFromCache(username);

            if (Cachedusers != null) {
                user = Cachedusers;
            }else {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                if (userDetails instanceof Users) {
                    user = UserPrincipalDto.fromEntity((Users) userDetails);
                    redisCacheService.setCacheUser(username, user);
                }
            }
            return user;
        }
        return null;
    }

    public void assertCanCreateProduct(UserPrincipalDto actor) {
        if (actor.getRole() == Role.SECONDARY_ADMIN) {
            long current = productRepo.countByAddedById(actor.getId());
            if (current >= SECONDARY_ADMIN_PRODUCT_LIMIT) {
                throw new IllegalStateException(
                        "Secondary admins can only add up to " + SECONDARY_ADMIN_PRODUCT_LIMIT + " products.");
            }
        }
    }

    public void assertCanModifyOrDeleteProduct(Product existing, UserPrincipalDto actor) {
        if (actor.getRole() == Role.SECONDARY_ADMIN) {
            Long ownerId = existing.getAddedBy() != null ? existing.getAddedBy().getId() : null;
            if (ownerId == null || !ownerId.equals(actor.getId())) {
                throw new IllegalStateException(
                        "Secondary admins can only modify or delete products they added.");
            }
        }
    }

    /**
     * Guard for batch product creation.
     * - ADMIN: unlimited.
     * - SECONDARY_ADMIN: total owned products (existing + incoming) must stay ≤ SECONDARY_ADMIN_PRODUCT_LIMIT.
     * - Others: denied.
     */
    public void assertCanBatchCreateProducts(UserPrincipalDto actor, int incomingCount) {
        if (actor == null) {
            throw new IllegalStateException("No authenticated admin.");
        }

        Role role = actor.getRole();

        if (role == Role.ADMIN) {
            return; // full admins: no cap
        }

        if (role == Role.SECONDARY_ADMIN) {
            long current = productRepo.countByAddedById(actor.getId());
            long total   = current + incomingCount;

            if (total > SECONDARY_ADMIN_PRODUCT_LIMIT) {
                long remaining = Math.max(0, SECONDARY_ADMIN_PRODUCT_LIMIT - current);
                throw new IllegalStateException(
                        "Secondary admins can only own up to " + SECONDARY_ADMIN_PRODUCT_LIMIT
                                + " products. You currently own " + current
                                + " and tried to add " + incomingCount
                                + " (remaining slots: " + remaining + ")."
                );
            }
            return;
        }

        throw new IllegalStateException("Only admins can batch-import products.");
    }
    
}
