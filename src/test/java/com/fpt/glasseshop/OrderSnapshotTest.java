package com.fpt.glasseshop;

import com.fpt.glasseshop.entity.*;
import com.fpt.glasseshop.entity.dto.*;
import com.fpt.glasseshop.service.*;
import com.fpt.glasseshop.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrderSnapshotTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private LensOptionRepository lensOptionRepository;

    @Autowired
    private CartService cartService;

    @Test
    @Transactional
    void testOrderSnapshotLogic() {
        // 1. Get a customer
        UserAccount customer = userAccountRepository.findByEmail("john.doe@example.com")
                .orElseGet(() -> userAccountRepository.save(UserAccount.builder()
                        .name("John Doe").email("john.doe@example.com").role("CUSTOMER").build()));

        // 2. Get a variant and lens
        ProductVariant variant = productVariantRepository.findAll().get(0);
        LensOption lens = lensOptionRepository.findAll().get(0);

        // 3. Add to cart
        AddToCartRequest addReq = AddToCartRequest.builder()
                .variantId(variant.getVariantId())
                .lensOptionId(lens.getLensOptionId())
                .isLens(true)
                .quantity(1)
                .sphLeft(new BigDecimal("-1.50"))
                .sphRight(new BigDecimal("-1.50"))
                .build();
        cartService.addToCart(customer, addReq);

        // 4. Checkout
        CreateOrderRequest checkoutReq = CreateOrderRequest.builder()
                .address("123 Main St")
                .fullName("John Doe")
                .phone("0123456789")
                .paymentMethod("COD")
                .build();

        OrderDTO order = orderService.createOrderFromCart(customer, checkoutReq);

        // 5. Verify Snapshots
        assertNotNull(order);
        assertEquals(1, order.getOrderItems().size());
        OrderItemDTO item = order.getOrderItems().get(0);

        System.out.println("Verifying snapshot for order: " + order.getOrderId());
        System.out.println("  Product Name: " + item.getProductName());
        System.out.println("  Lens Type: " + item.getLensType());

        assertEquals(variant.getProduct().getName(), item.getProductName());
        assertEquals(lens.getType(), item.getLensType());
        assertEquals(lens.getPrice(), item.getLensPrice());

        System.out.println("SNAPSHOT TEST PASSED!");
    }
}
