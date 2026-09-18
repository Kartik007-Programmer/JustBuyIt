package com.example.JustBuyIt.Services;

import com.example.JustBuyIt.DTOs.CartItemResponse;
import com.example.JustBuyIt.DTOs.PaymentRequest;
import com.example.JustBuyIt.DTOs.PaymentResponse;
import com.example.JustBuyIt.Models.ShoppingCart;
import com.example.JustBuyIt.Models.Users;
import com.example.JustBuyIt.Repository.ShoppingCartRepo;
import com.example.JustBuyIt.Repository.UsersRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final ShoppingCartService CartService;
    private final ShoppingCartRepo CartRepo;
    private final UsersRepo UsersRepo;

    public PaymentService(ShoppingCartService cartService, ShoppingCartRepo cartRepo, UsersRepo usersRepo) {
        CartService = cartService;
        CartRepo = cartRepo;
        UsersRepo = usersRepo;
    }

    @Transactional
    public PaymentResponse processFakePayment(String email, PaymentRequest paymentRequest) {
        Users user = UsersRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found!"));

        ShoppingCart cart = CartRepo.findByUserId(user.getId()).orElseThrow(() -> new RuntimeException("Cart not found!"));

        if (cart.getCartItems().isEmpty() || cart.getTotalPrice() <= 0) {
            throw new RuntimeException("Cart not found!");
        }

        Double TotalAmountInINR = cart.getTotalPrice();

        String TransactionId = "TXN_INR_"+UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // ① SNAPSHOT — copy items into plain DTOs while cart is still populated
        List<CartItemResponse> snapshot = cart.getCartItems().stream()
                .map(ci -> new CartItemResponse(
                        ci.getProduct().getName(),
                        ci.getQuantity(),
                        ci.getProduct().getPrice(),
                        ci.getQuantity() * ci.getProduct().getPrice()))
                .toList();

        CartService.clearCart(email);

        return new PaymentResponse(
                TransactionId,
                TotalAmountInINR,
                "SUCCESS",
                "Fake Payment of ₹" + TotalAmountInINR + " processed successfully in Indian Rupees",
                paymentRequest.getPaymentMethod(),
                snapshot);
    }
}
