package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.*;
import com.fpt.glasseshop.entity.dto.CreateReviewRequest;
import com.fpt.glasseshop.entity.dto.ReviewDTO;
import com.fpt.glasseshop.exception.ResourceNotFoundException;
import com.fpt.glasseshop.repository.OrderRepository;
import com.fpt.glasseshop.repository.ProductRepository;
import com.fpt.glasseshop.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    @Transactional
    public ReviewDTO createReview(UserAccount user, CreateReviewRequest request) {
        // 1. Get Order and Validate Ownership
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.getOrderId()));

        if (!order.getUser().getUserId().equals(user.getUserId())) {
            throw new org.springframework.security.access.AccessDeniedException("You can only review products from your own orders");
        }

        // 2. Validate Order Status (Only Delivered orders can be reviewed)
        if (!"DELIVERED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalArgumentException("You can only review products once the order has been DELIVERED");
        }

        // 3. Find Product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        // 4. Verify Product was in that Order
        boolean productInOrder = order.getOrderItems().stream()
                .anyMatch(item -> (item.getVariant() != null && item.getVariant().getProduct().getProductId().equals(product.getProductId()))
                        || (item.getProductId() != null && item.getProductId().equals(product.getProductId())));

        if (!productInOrder) {
            throw new IllegalArgumentException("This product was not part of order: " + order.getOrderCode());
        }

        // 5. Uniqueness Check (User can only review a product once for a specific order)
        if (reviewRepository.existsByUserUserIdAndProductProductIdAndOrderOrderId(user.getUserId(), product.getProductId(), order.getOrderId())) {
            throw new IllegalArgumentException("You have already reviewed this product for this order");
        }

        // 6. Create Review
        Review review = Review.builder()
                .user(user)
                .product(product)
                .order(order)
                .orderCode(order.getOrderCode())
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .build();

        Review saved = reviewRepository.save(review);

        // Notify admins
        notificationService.notifyAdmins(
            "New Product Review", 
            user.getName() + " gave " + request.getRating() + " stars to " + product.getName(),
            "PRODUCT",
            product.getProductId()
        );

        return convertToDTO(saved);
    }

    public List<ReviewDTO> getReviewsByProduct(Long productId) {
        return reviewRepository.findByProductProductId(productId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ReviewDTO> getReviewsByUser(Long userId) {
        return reviewRepository.findByUserUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ReviewDTO convertToDTO(Review review) {
        return ReviewDTO.builder()
                .reviewId(review.getReviewId())
                .productId(review.getProduct().getProductId())
                .productName(review.getProduct().getName())
                .userId(review.getUser().getUserId())
                .userName(review.getUser().getName())
                .orderId(review.getOrder().getOrderId())
                .orderCode(review.getOrderCode())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
