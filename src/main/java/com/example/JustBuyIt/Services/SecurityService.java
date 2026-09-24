package com.example.JustBuyIt.Services;

import com.example.JustBuyIt.Models.Role;
import com.example.JustBuyIt.Models.Users;
import com.example.JustBuyIt.Repository.UsersRepo;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@Service
public class SecurityService {


    private final JwtService jwtService;

    private final UsersRepo usersRepo;

    private final AuthenticationManager authenticationManager;

    private PasswordEncoder passwordEncoder;

    private final UserDetailsService userDetailsService;

    private final RedisCacheService redisCacheService;

    private final EmailService  emailService;

    public SecurityService(JwtService jwtService, UsersRepo usersRepo, AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder, UserDetailsService userDetailsService, RedisCacheService redisCacheService, EmailService emailService) {
        this.jwtService = jwtService;
        this.usersRepo = usersRepo;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.redisCacheService = redisCacheService;
        this.emailService = emailService;
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

        passwordEncoder = new BCryptPasswordEncoder();
        users.setPassword(passwordEncoder.encode(users.getPassword()));
        users.setEmailVerified(false);

        String token = UUID.randomUUID().toString();
        users.setVerificationToken(token);
        users.setVerificationTokenExpiry(LocalDateTime.now().plusMinutes(1440));

        Users savedUser = usersRepo.save(users);
        // Cache the new user
        redisCacheService.setCacheUser(savedUser.getEmail(), savedUser);

        try {
            emailService.sendVerificationEmail(savedUser.getEmail(), token);
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
        redisCacheService.setCacheUser(user.getEmail(), user);

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
            redisCacheService.setCacheUser(user.getEmail(), user);

            try {
                emailService.sendPasswordResetEmail(user.getEmail(), token);
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
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        usersRepo.save(user);
        redisCacheService.setCacheUser(user.getEmail(), user);

        return ResponseEntity.ok("Password reset successful");
    }

    public ResponseEntity<?> VerifyUserByUsernamePassword(String username, String password, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(username, password));
            if (authentication.isAuthenticated()) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                String token = jwtService.generateToken(userDetails);

                System.out.println("token: " + token);

                // Cache the user and token in Redis
                redisCacheService.setCacheUser(username, (Users) userDetails);
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

    public ResponseEntity<?> Logout(HttpServletResponse response) {
        // Get current authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            // Invalidate cache
            redisCacheService.invalidateUserCache(userDetails.getUsername());
        }

        ResponseCookie cookie = ResponseCookie.from("JWT_TOKEN", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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

    public Users getPresentAuthorizedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Users user = null;
        assert authentication != null;
        String username = authentication.getName();

        Users Cachedusers = redisCacheService.getUserFromCache(username);

        if (Cachedusers != null) {
            user = Cachedusers;
        }else {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            if (userDetails instanceof Users) {
                user = (Users) userDetails;
                redisCacheService.setCacheUser(username, user);
            }
        }
        return user;
    }

    public Users getPresentAuthorizedAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Users user = null;
        assert authentication != null;
        String username = authentication.getName();
        String role = getPresentAuthorizedRole();
        if (role.equals("ADMIN")) {
            Users Cachedusers = redisCacheService.getUserFromCache(username);

            if (Cachedusers != null) {
                user = Cachedusers;
            }else {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                if (userDetails instanceof Users) {
                    user = (Users) userDetails;
                    redisCacheService.setCacheUser(username, user);
                }
            }
            return user;
        }
        return null;
    }
}
