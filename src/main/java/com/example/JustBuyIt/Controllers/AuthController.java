package com.example.JustBuyIt.Controllers;

import com.example.JustBuyIt.Models.Users;
import com.example.JustBuyIt.Services.SecurityService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/v1")
public class AuthController {

    @Autowired
    SecurityService securityService;

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
}
