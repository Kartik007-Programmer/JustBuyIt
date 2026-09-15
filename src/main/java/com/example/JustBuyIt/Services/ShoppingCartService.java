package com.example.JustBuyIt.Services;

import com.example.JustBuyIt.DTOs.CartItemRequest;
import com.example.JustBuyIt.DTOs.CartItemResponse;
import com.example.JustBuyIt.DTOs.ShoppingCartResponse;
import com.example.JustBuyIt.Models.CartItem;
import com.example.JustBuyIt.Models.Product;
import com.example.JustBuyIt.Models.ShoppingCart;
import com.example.JustBuyIt.Models.Users;
import com.example.JustBuyIt.Repository.ProductRepo;
import com.example.JustBuyIt.Repository.ShoppingCartRepo;
import com.example.JustBuyIt.Repository.UsersRepo;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShoppingCartService {
    private final ShoppingCartRepo cartRepo;
    private final UsersRepo usersRepo;
    private final ProductRepo productRepo;

    public ShoppingCartService(ShoppingCartRepo shoppingCartRepo, UsersRepo usersRepo, ProductRepo productRepo) {
        this.cartRepo = shoppingCartRepo;
        this.usersRepo = usersRepo;
        this.productRepo = productRepo;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "cart", key = "#Email")
    public ShoppingCartResponse getCartByUser(String Email){
        Users users = usersRepo.findByEmail(Email).orElseThrow(() -> new RuntimeException("User Not Found!"));
        ShoppingCart cart = getOrCreateCart(users);
        return mapToCartResponse(cart);
    }

    @Transactional
    @CachePut(value = "cart", key = "#Email")
    public ShoppingCartResponse addItemToCart(String Email, CartItemRequest cartItemRequest){
        Users  users = usersRepo.findByEmail(Email).orElseThrow(() -> new RuntimeException("User Not Found!"));
        Product product = productRepo.findById(cartItemRequest.getProductId()).orElseThrow(() -> new RuntimeException("Product Not Found!"));

        if (product.getQuantity() < cartItemRequest.getQuantity()){
            throw new RuntimeException("Product Quantity Not Enough!");
        }

        ShoppingCart cart = getOrCreateCart(users);

        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item ->
                        item.getProduct().getId().equals(product.getId())).findFirst();
        if (existingItem.isPresent()){
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + cartItemRequest.getQuantity();
            if (product.getQuantity() < newQuantity) {
                throw new RuntimeException("Product Quantity Not Enough!");
            }
            item.setQuantity(newQuantity);
            item.setPrice(item.getQuantity()*product.getPrice());
        }else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(cartItemRequest.getQuantity());
            newItem.setPrice(cartItemRequest.getQuantity() * product.getPrice());
            cart.getCartItems().add(newItem);
        }

        reCalculateCartTotal(cart);
        ShoppingCart SaveCart = cartRepo.save(cart);
        return mapToCartResponse(SaveCart);
    }

    @Transactional
    @CachePut(value = "cart", key = "#Email")
    public ShoppingCartResponse removeItemFromCart(String Email, Long CartItemId){
        ShoppingCart cart = getUserCart(Email);

        cart.getCartItems().removeIf(Item -> Item.getId().equals(CartItemId));

        reCalculateCartTotal(cart);

        return mapToCartResponse(cartRepo.save(cart));
    }

    @Transactional
    @CacheEvict(value = "cart", key = "#Email")
    public void clearCart(String Email){
        ShoppingCart cart = getUserCart(Email);
        cart.getCartItems().clear();
        cart.setTotalPrice(0.0);
        cartRepo.save(cart);
    }

    private ShoppingCart getUserCart(String email) {
        Users users = usersRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User Not Found!"));
        return cartRepo.findByUserId(users.getId()).orElseThrow(() -> new RuntimeException("User not found!"));
    }

    private void reCalculateCartTotal(ShoppingCart cart) {
        double total = cart.getCartItems()
                .stream()
                .mapToDouble(CartItem::getPrice)
                .sum();
        cart.setTotalPrice(total);
    }

    private ShoppingCartResponse mapToCartResponse(ShoppingCart cart) {
        List<CartItemResponse> itemResponsesList = cart.getCartItems().stream().map(item -> new CartItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getPrice(),
                item.getQuantity(),
                item.getPrice()
        )).collect(Collectors.toList());

        return new ShoppingCartResponse(cart.getId(),itemResponsesList,cart.getTotalPrice());
    }

    private ShoppingCart getOrCreateCart(Users user) {
        return cartRepo.findByUserId(user.getId()).orElseGet(() -> {
            ShoppingCart cart = new ShoppingCart();
            cart.setUser(user);
            return cartRepo.save(cart);
        });
    }
}
