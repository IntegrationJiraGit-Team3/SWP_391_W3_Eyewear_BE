package com.fpt.glasseshop.controller;

import com.fpt.glasseshop.entity.UserAccount;
import com.fpt.glasseshop.entity.dto.ApiResponse;
import com.fpt.glasseshop.entity.dto.CreateReviewRequest;
import com.fpt.glasseshop.entity.dto.ReviewDTO;
import com.fpt.glasseshop.repository.UserAccountRepository;
import com.fpt.glasseshop.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "ReviewAPI", description = "Operations related to product reviews")
public class ReviewRestController {

    private final ReviewService reviewService;
    private final UserAccountRepository userAccountRepository;

    // ✅ Lấy user từ JWT
    private UserAccount getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        if (email == null) {
            throw new AccessDeniedException("User is not authenticated");
        }

        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User not found"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewDTO>> createReview(
            @Valid @RequestBody CreateReviewRequest req) {

        try {
            UserAccount user = getCurrentUser();

            ReviewDTO reviewDTO = reviewService.createReview(user, req);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Review submitted successfully", reviewDTO));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error creating review: " + e.getMessage()));
        }
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<ReviewDTO>>> getReviewsByProduct(
            @PathVariable Long productId) {

        List<ReviewDTO> reviews = reviewService.getReviewsByProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<ReviewDTO>>> getReviewsByUser(
            @PathVariable Long userId) {

        List<ReviewDTO> reviews = reviewService.getReviewsByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }
}