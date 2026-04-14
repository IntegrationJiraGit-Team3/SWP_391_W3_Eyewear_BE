package com.fpt.glasseshop;

import com.fpt.glasseshop.entity.*;
import com.fpt.glasseshop.entity.dto.*;
import com.fpt.glasseshop.service.*;
import com.fpt.glasseshop.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PreOrderTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private CartService cartService;

    @Test
    @Transactional
    void testPreOrderFlow() {
        // 1. Get or create a customer
        UserAccount customer = userAccountRepository.findByEmail("tester@example.com")
                .orElseGet(() -> userAccountRepository.save(UserAccount.builder()
                        .name("Tester").email("tester@example.com").role("CUSTOMER").build()));

        // 2. Get a variant
        ProductVariant variant = productVariantRepository.findAll().stream().findFirst()
            .orElseThrow(() -> new RuntimeException("No product variant found in database for testing"));

        // 3. Add to cart as a Pre-Order item
        AddToCartRequest addReq = AddToCartRequest.builder()
                .variantId(variant.getVariantId())
                .productId(variant.getProduct().getProductId())
                .isLens(false)
                .isPreorder(true)
                .quantity(1)
                .build();
        cartService.addToCart(customer, addReq);

        // 4. Verify cart item is marked as pre-order
        CartDTO cart = cartService.getCart(customer);
        assertEquals(1, cart.getItems().size());
        assertTrue(Boolean.TRUE.equals(cart.getItems().get(0).getIsPreorder()));

        // 5. Checkout
        CreateOrderRequest checkoutReq = CreateOrderRequest.builder()
                .address("Test Address")
                .fullName("Test Customer")
                .phone("0987654321")
                .paymentMethod("COD")
                .build();

        OrderDTO order = orderService.createOrderFromCart(customer, checkoutReq);

        // 6. Verify order item is marked as pre-order and fulfillment correct
        assertNotNull(order);
        assertEquals(1, order.getOrderItems().size());
        OrderItemDTO item = order.getOrderItems().get(0);
        
        System.out.println("Processing Order ID: " + order.getOrderId());
        System.out.println("Item Fulfillment Type: " + item.getFulfillmentType());
        System.out.println("Item isPreorder: " + item.getIsPreorder());

        assertTrue(Boolean.TRUE.equals(item.getIsPreorder()), "Order item should be marked as isPreorder=true");
        assertEquals("PRE_ORDER", item.getFulfillmentType(), "Fulfillment type should be PRE_ORDER");

        System.out.println("PRE-ORDER FLOW TEST PASSED!");
    }
}
