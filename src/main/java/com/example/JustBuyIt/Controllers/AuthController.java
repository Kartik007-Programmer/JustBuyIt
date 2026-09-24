package com.example.JustBuyIt.Controllers;

import com.example.JustBuyIt.Models.Users;
import com.example.JustBuyIt.Services.SecurityService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/v1")
public class AuthController {

    private final SecurityService securityService;

    public AuthController(SecurityService securityService) {
        this.securityService = securityService;
    }

    //    @Autowired
//    ModelAndView mv;

    @PostMapping("/register")
    ResponseEntity<?> Register(@RequestBody Users users) {
        return securityService.RegisterUser(users);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestParam("username") String username,
            @RequestParam("password")  String password,
            HttpServletResponse response){

        return securityService.VerifyUserByUsernamePassword(username,password,response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        return securityService.Logout(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        ResponseEntity<?> result = securityService.verifyEmail(token);
        String status = result.getStatusCode().is2xxSuccessful() ? "success" : "failed";
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/LoginForm.html?verified=" + status)
                .build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam("email") String email) {
        return securityService.requestPasswordReset(email);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestParam("token") String token,
            @RequestParam("password") String password) {
        return securityService.resetPassword(token, password);
    }

}
