package com.example.JustBuyIt.Controllers;

import com.example.JustBuyIt.DTOs.CartItemRequest;
import com.example.JustBuyIt.DTOs.PaymentRequest;
import com.example.JustBuyIt.DTOs.PaymentResponse;
import com.example.JustBuyIt.DTOs.ShoppingCartResponse;
import com.example.JustBuyIt.Models.ShoppingCart;
import com.example.JustBuyIt.Services.PaymentService;
import com.example.JustBuyIt.Services.ReceiptService;
import com.example.JustBuyIt.Services.ShoppingCartService;
import com.example.JustBuyIt.Services.UsersService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("users/cart")
public class ShoppingCartController {
    private final ShoppingCartService cartService;
    private final PaymentService paymentService;
    private final ReceiptService  receiptService;
    private final UsersService usersService;

    public ShoppingCartController(ShoppingCartService cartService, PaymentService paymentService, ReceiptService receiptService, UsersService usersService) {
        this.cartService = cartService;
        this.paymentService = paymentService;
        this.receiptService = receiptService;
        this.usersService = usersService;
    }

    @GetMapping
    ResponseEntity<ShoppingCartResponse> getShoppingCart(Authentication authentication) {
        return ResponseEntity.ok().body(cartService.getCartByUser(authentication.getName()));
    }

    @PostMapping("/items")
    ResponseEntity<?> addItemToCart(Authentication authentication, @RequestBody CartItemRequest itemRequest){
        try {
            ShoppingCartResponse cart = cartService.addItemToCart(authentication.getName(),itemRequest);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
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

    @PostMapping("/payment/checkout/receipt")
    public ResponseEntity<byte[]> downloadReceipt(
            Principal principal,
            @RequestBody PaymentResponse payment) {

        // ① Guard: snapshot must be present
        if (payment.getItems() == null || payment.getItems().isEmpty()) {
            System.out.println("Receipt requested for {} with empty items: "+ payment.getTransactionId());
            return ResponseEntity.badRequest().body("No items in payment snapshot".getBytes());
        }

        ShoppingCartResponse order = new ShoppingCartResponse();

        order.setItems(payment.getItems());
        order.setTotalPrice(payment.getAmount());

        byte[] pdf = receiptService.generateReceiptPdf(payment, order, usersService.getUserByEmail(principal.getName()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition
                .attachment() // Attachment forces file download instead of inline view
                .filename("receipt-" + payment.getPaymentMethod() + ".pdf")
                .build());

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

}
