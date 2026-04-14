package com.fpt.glasseshop.controller;

import com.fpt.glasseshop.entity.UserAccount;
import com.fpt.glasseshop.entity.dto.AddToCartRequest;
import com.fpt.glasseshop.entity.dto.CartDTO;
import com.fpt.glasseshop.entity.dto.CartItemDTO;
import com.fpt.glasseshop.repository.UserAccountRepository;
import com.fpt.glasseshop.service.CartService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartRestController {

    private final CartService cartService;
    private final UserAccountRepository userAccountRepository;

    // ✅ LẤY USER TỪ JWT
    private UserAccount getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public ResponseEntity<CartDTO> getCart() {
        UserAccount user = getCurrentUser();
        return ResponseEntity.ok(cartService.getCart(user));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CartItemDTO>> getCartItemsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCartItemsByUserId(userId));
    }

    @PostMapping("/add")
    public ResponseEntity<CartDTO> addToCart(@Valid @RequestBody AddToCartRequest req) {
        UserAccount user = getCurrentUser();
        return ResponseEntity.ok(cartService.addToCart(user, req));
    }

    @PutMapping("/item/{cartItemId}")
    public ResponseEntity<CartDTO> updateQuantity(
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity) {

        UserAccount user = getCurrentUser();
        return ResponseEntity.ok(cartService.updateQuantity(user, cartItemId, quantity));
    }

    @DeleteMapping("/item/{cartItemId}")
    public ResponseEntity<CartDTO> removeCartItem(@PathVariable Long cartItemId) {
        UserAccount user = getCurrentUser();
        return ResponseEntity.ok(cartService.removeCartItem(user, cartItemId));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart() {
        UserAccount user = getCurrentUser();
        cartService.clearCart(user);
        return ResponseEntity.ok().build();
    }
}