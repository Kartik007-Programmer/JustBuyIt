package com.example.JustBuyIt.Services;

import com.example.JustBuyIt.DTOs.CartItemResponse;
import com.example.JustBuyIt.DTOs.PaymentRequest;
import com.example.JustBuyIt.DTOs.PaymentResponse;
import com.example.JustBuyIt.Exceptions.ApiException;
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
    private final OrderService orderService;

    public PaymentService(ShoppingCartService cartService, ShoppingCartRepo cartRepo, UsersRepo usersRepo, OrderService orderService) {
        CartService = cartService;
        CartRepo = cartRepo;
        UsersRepo = usersRepo;
        this.orderService = orderService;
    }

    @Transactional
    public PaymentResponse processFakePayment(String email, PaymentRequest paymentRequest) {
        Users user = UsersRepo.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User Not Found!"));

        // ⬇ Require delivery info before letting payment go through
        if (user.getPhone() == null || user.getPhone().isBlank()
                || user.getAddress() == null || user.getAddress().isBlank()) {
            throw ApiException.badRequest(
                    "Please add your phone and delivery address in your profile before checking out.");
        }

        ShoppingCart cart = CartRepo.findByEmailForUpdate(email)
                .orElseThrow(() -> ApiException.notFound("Cart not found!"));

        if (cart.getCartItems().isEmpty() || cart.getTotalPrice() <= 0) {
            throw ApiException.badRequest("Cart is empty.");
        }

        Double totalAmount = cart.getTotalPrice();
        String transactionId = "TXN_INR_" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase();

        // Snapshot BEFORE clearing (order service copies the items)
        List<CartItemResponse> snapshot = cart.getCartItems().stream()
                .map(ci -> new CartItemResponse(
                        ci.getId(),
                        ci.getProduct().getId(),
                        ci.getProduct().getName(),
                        ci.getProduct().getPrice(),
                        ci.getQuantity(),
                        ci.getPrice()))
                .toList();

        // Persist the order + decrement stock
        orderService.createOrderFromCart(
                user, cart, transactionId, paymentRequest.getPaymentMethod());

        // Empty the cart
        CartService.clearCart(email);

        return new PaymentResponse(
                transactionId,
                totalAmount,
                "SUCCESS",
                "Fake Payment of ₹" + totalAmount + " processed successfully in Indian Rupees",
                paymentRequest.getPaymentMethod(),
                snapshot);
    }
}
