package com.example.JustBuyIt.Controllers;

import com.example.JustBuyIt.DTOs.CartItemRequest;
import com.example.JustBuyIt.DTOs.PaymentRequest;
import com.example.JustBuyIt.DTOs.PaymentResponse;
import com.example.JustBuyIt.DTOs.ShoppingCartResponse;
import com.example.JustBuyIt.Services.PaymentService;
import com.example.JustBuyIt.Services.ShoppingCartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("users/cart")
public class ShoppingCartController {
    private final ShoppingCartService cartService;
    private final PaymentService paymentService;

    public ShoppingCartController(ShoppingCartService cartService, PaymentService paymentService) {
        this.cartService = cartService;
        this.paymentService = paymentService;
    }

    @GetMapping
    ResponseEntity<ShoppingCartResponse> getShoppingCart(Authentication authentication) {
        return ResponseEntity.ok().body(cartService.getCartByUser(authentication.getName()));
    }

    @PostMapping("/items")
    ResponseEntity<ShoppingCartResponse> addItemToCart(Authentication authentication, @RequestBody CartItemRequest itemRequest){
        return ResponseEntity.ok().body(cartService.addItemToCart(authentication.getName(),itemRequest));
    }

    @DeleteMapping("/items/{cartItemId}")
    ResponseEntity<ShoppingCartResponse> removeItemFromCart(
            Authentication authentication,
            @PathVariable("cartItemId") Long cartItemId){
        return ResponseEntity.ok().body(cartService.removeItemFromCart(authentication.getName(),cartItemId));
    }

    @DeleteMapping
    ResponseEntity<String> clearCart(Authentication authentication){
        cartService.clearCart(authentication.getName());
        return ResponseEntity.ok().body("Cart has been cleared!");
    }

    @PostMapping("/payment/checkout")
    public ResponseEntity<PaymentResponse> processCheckout(Principal principal, @RequestBody PaymentRequest paymentRequest) {
        PaymentResponse response = paymentService.processFakePayment(principal.getName(), paymentRequest);
        return ResponseEntity.ok(response);
    }

}
