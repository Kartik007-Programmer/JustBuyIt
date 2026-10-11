package com.example.JustBuyIt.Services;

import com.example.JustBuyIt.DTOs.*;
import com.example.JustBuyIt.Exceptions.ApiException;
import com.example.JustBuyIt.Models.*;
import com.example.JustBuyIt.Repository.OrderRepo;
import com.example.JustBuyIt.Repository.ProductRepo;
import com.example.JustBuyIt.Repository.UsersRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepo orderRepo;
    private final UsersRepo usersRepo;
    private final ShoppingCartService cartService;
    private final ProductRepo productRepo;
    private final ProductService productService;

    public OrderService(OrderRepo orderRepo,
                        UsersRepo usersRepo,
                        ShoppingCartService cartService,
                        ProductRepo productRepo,
                        ProductService productService) {
        this.orderRepo = orderRepo;
        this.usersRepo = usersRepo;
        this.cartService = cartService;
        this.productRepo = productRepo;
        this.productService = productService;
    }

    /* ============================================================
           CREATE ORDER FROM CART  —  called by PaymentService
           ============================================================ */
    @Transactional
    public Order createOrderFromCart(Users user, ShoppingCart cart,
                                     String transactionId, String paymentMethod) {

        if (cart.getCartItems().isEmpty()) {
            throw ApiException.badRequest("Cart is empty.");
        }

        Order order = new Order();
        order.setUser(user);
        order.setTransactionId(transactionId);
        order.setTotalAmount(cart.getTotalPrice());
        order.setPaymentMethod(paymentMethod);
        order.setStatus(OrderStatus.PLACED);
        order.setDeliveryName(
                user.getName() != null && !user.getName().isBlank()
                        ? user.getName()
                        : user.getEmail()
        );
        order.setDeliveryPhone(user.getPhone());
        order.setDeliveryAddress(user.getAddress());

        for (CartItem ci : cart.getCartItems()) {
            Product p = ci.getProduct();
            Integer currentQty = p.getQuantity();
            Integer orderedQty = ci.getQuantity();

            if (currentQty == null) currentQty = 0;
            if (orderedQty == null || orderedQty < 1) {
                throw ApiException.badRequest("Invalid quantity in cart.");
            }

            if (currentQty < orderedQty) {
                throw ApiException.badRequest(
                        "Not enough stock for " + p.getName()
                                + " (requested " + orderedQty
                                + ", available " + currentQty + ").");
            }

            // Snapshot the order line
            OrderItem oi = new OrderItem(
                    order,
                    p.getId(),
                    p.getName(),
                    p.getPrice(),
                    orderedQty,
                    ci.getPrice()
            );
            order.getItems().add(oi);
            p.setQuantity(currentQty - orderedQty);
            productRepo.save(p);
        }
        productService.bumpVersion();
        return orderRepo.save(order);
    }

    /* ============================================================
       READ
       ============================================================ */
    @Transactional(readOnly = true)
    public List<OrderSummaryDTO> getOrdersFor(String email) {
        Users user = usersRepo.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User not found."));

        return orderRepo.findAllByUserId(user.getId())
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> getOrdersFor(String email, Pageable pageable) {
        Users user = usersRepo.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User not found."));

        return orderRepo.findPageByUserId(user.getId(), pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public OrderDetailDTO getOrderDetail(String email, Long orderId) {
        Users user = usersRepo.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User not found."));

        Order order = orderRepo.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> ApiException.notFound("Order not found."));

        return toDetail(order);
    }

    /* ============================================================
       MAPPERS
       ============================================================ */
    private OrderSummaryDTO toSummary(Order o) {
        int itemCount = o.getItems().stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        return new OrderSummaryDTO(
                o.getId(),
                o.getTransactionId(),
                o.getTotalAmount(),
                o.getPaymentMethod(),
                o.getStatus().name(),
                o.getPlacedAt(),
                itemCount
        );
    }

    private OrderDetailDTO toDetail(Order o) {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setId(o.getId());
        dto.setTransactionId(o.getTransactionId());
        dto.setTotalAmount(o.getTotalAmount());
        dto.setPaymentMethod(o.getPaymentMethod());
        dto.setStatus(o.getStatus().name());
        dto.setPlacedAt(o.getPlacedAt());
        dto.setDeliveryName(o.getDeliveryName());
        dto.setDeliveryAddress(o.getDeliveryAddress());
        dto.setDeliveryPhone(o.getDeliveryPhone());

        List<OrderItemDTO> itemDTOs = o.getItems().stream()
                .map(i -> new OrderItemDTO(
                        i.getProductId(),
                        i.getProductName(),
                        i.getUnitPrice(),
                        i.getQuantity(),
                        i.getSubTotal()))
                .toList();
        dto.setItems(itemDTOs);
        return dto;
    }
}