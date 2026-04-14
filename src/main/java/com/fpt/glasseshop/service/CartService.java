package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.Cart;
import com.fpt.glasseshop.entity.CartItem;
import com.fpt.glasseshop.entity.LensOption;
import com.fpt.glasseshop.entity.ProductVariant;
import com.fpt.glasseshop.entity.UserAccount;
import com.fpt.glasseshop.entity.dto.AddToCartRequest;
import com.fpt.glasseshop.entity.dto.CartDTO;
import com.fpt.glasseshop.entity.dto.CartItemDTO;
import com.fpt.glasseshop.exception.ResourceNotFoundException;
import com.fpt.glasseshop.repository.CartItemRepository;
import com.fpt.glasseshop.repository.CartRepository;
import com.fpt.glasseshop.repository.LensOptionRepository;
import com.fpt.glasseshop.repository.PrescriptionRepository;
import com.fpt.glasseshop.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final LensOptionRepository lensOptionRepository;
    private final PrescriptionRepository prescriptionRepository;

    @Transactional
    public CartDTO getCart(UserAccount user) {
        Cart cart = getOrCreateCart(user);
        return convertToDTO(cart);
    }

    @Transactional(readOnly = true)
    public List<CartItemDTO> getCartItemsByUserId(Long userId) {
        return cartRepository.findByUserUserId(userId)
                .map(cart -> convertToDTO(cart).getItems())
                .orElse(new ArrayList<>());
    }

    @Transactional
    public CartDTO addToCart(UserAccount user, AddToCartRequest request) {
        Cart cart = getOrCreateCart(user);

        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product Variant not found with id: " + request.getVariantId()));

        LensOption lensOption = null;
        if (request.getLensOptionId() != null) {
            lensOption = lensOptionRepository.findById(request.getLensOptionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Lens Option not found with id: " + request.getLensOptionId()));
        }

        // Check if item already exists in cart with same variant AND SAME prescription
        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> {
                    boolean sameProduct = item.getProductId() != null &&
                                          request.getProductId() != null &&
                                          item.getProductId().equals(request.getProductId()) &&
                                          item.getVariant().getVariantId().equals(variant.getVariantId());
                    if (!sameProduct) return false;

                    // If same product, check if same lens/prescription context
                    boolean bothPreorder = Boolean.TRUE.equals(item.getIsPreorder()) && Boolean.TRUE.equals(request.getIsPreorder());
                    if (bothPreorder) return true; // Preorders with same variant can merge

                    // Check Lens Option
                    boolean sameLensOption = (item.getLensOption() == null && request.getLensOptionId() == null) ||
                                             (item.getLensOption() != null && request.getLensOptionId() != null && 
                                              item.getLensOption().getLensOptionId().equals(request.getLensOptionId()));
                    if (!sameLensOption) return false;

                    // Check Prescription Uniqueness
                    if (item.getPrescription() == null && request.getPrescriptionId() == null && request.getSphLeft() == null) {
                        return true; // Both plain
                    }

                    // If one has prescription and other doesn't, skip
                    if ((item.getPrescription() == null) != (request.getPrescriptionId() == null && request.getSphLeft() == null && Boolean.FALSE.equals(request.getIsLens()))) {
                        return false;
                    }

                    // For now, if either has prescription, treat as separate or implement deep check
                    // Usually different prescriptions = different cart items
                    return false; 
                })
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            if (request.getIsLens() != null) existingItem.setIsLens(request.getIsLens());
            if (request.getIsPreorder() != null) existingItem.setIsPreorder(request.getIsPreorder());
            if (lensOption != null) existingItem.setLensOption(lensOption);
            cartItemRepository.save(existingItem);
        } else {
            java.math.BigDecimal variantPriceVal = variant.getProduct() != null && variant.getProduct().getPrice() != null ? variant.getProduct().getPrice() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal lensPriceVal = lensOption != null && lensOption.getPrice() != null ? lensOption.getPrice() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal price = variantPriceVal.add(lensPriceVal);

            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .lensOption(lensOption)
                    .quantity(request.getQuantity())
                    .productId(request.getProductId())
                    .productName(variant.getProduct() != null ? variant.getProduct().getName() : null)
                    .price(price)
                    .isLens(request.getIsLens() != null ? request.getIsLens() : false)
                    .isPreorder(request.getIsPreorder() != null ? request.getIsPreorder() : false)
                    .build();

            if (Boolean.TRUE.equals(request.getIsLens())) {
                java.math.BigDecimal sphL = request.getSphLeft();
                java.math.BigDecimal sphR = request.getSphRight();
                java.math.BigDecimal cylL = request.getCylLeft();
                java.math.BigDecimal cylR = request.getCylRight();
                Integer axisL = request.getAxisLeft();
                Integer axisR = request.getAxisRight();
                java.math.BigDecimal addL = request.getAddLeft();
                java.math.BigDecimal addR = request.getAddRight();


                if (request.getPrescriptionId() != null) {
                    com.fpt.glasseshop.entity.Prescription savedP = prescriptionRepository.findById(request.getPrescriptionId())
                            .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));
                    sphL = savedP.getSphLeft();
                    sphR = savedP.getSphRight();
                    cylL = savedP.getCylLeft();
                    cylR = savedP.getCylRight();
                    axisL = savedP.getAxisLeft();
                    axisR = savedP.getAxisRight();
                    addL = savedP.getAddLeft();
                    addR = savedP.getAddRight();

                }

                com.fpt.glasseshop.entity.Prescription prescription = com.fpt.glasseshop.entity.Prescription.builder()
                        .sphLeft(sphL)
                        .sphRight(sphR)
                        .cylLeft(cylL)
                        .cylRight(cylR)
                        .axisLeft(axisL)
                        .axisRight(axisR)
                        .addLeft(addL)
                        .addRight(addR)

                        .status(false) // pending approval
                        .cartItem(newItem)
                        .build();
                newItem.setPrescription(prescription);
            }

            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        return convertToDTO(cartRepository.save(cart));
    }

    @Transactional
    public CartDTO updateQuantity(UserAccount user, Long cartItemId, Integer quantity) {
        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem not found with id: " + cartItemId));

        if (!item.getCart().getCartId().equals(cart.getCartId())) {
            throw new IllegalArgumentException("CartItem does not belong to the user's cart");
        }

        if (quantity <= 0) {
            return removeCartItem(user, cartItemId);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return convertToDTO(cartRepository.findById(cart.getCartId()).orElse(cart));
    }

    @Transactional
    public CartDTO removeCartItem(UserAccount user, Long cartItemId) {
        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem not found with id: " + cartItemId));

        if (!item.getCart().getCartId().equals(cart.getCartId())) {
            throw new IllegalArgumentException("CartItem does not belong to the user's cart");
        }

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        return convertToDTO(cartRepository.save(cart));
    }

    private Cart getOrCreateCart(UserAccount user) {
        return cartRepository.findByUserUserId(user.getUserId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .items(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    @Transactional
    public void clearCart(UserAccount user) {
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private CartDTO convertToDTO(Cart cart) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalItems = 0;

        List<CartItemDTO> itemDTOs = new ArrayList<>();
        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                BigDecimal variantPrice = (item.getVariant().getProduct() != null && item.getVariant().getProduct().getPrice() != null) ? item.getVariant().getProduct().getPrice()
                        : BigDecimal.ZERO;
                BigDecimal lensPrice = (item.getLensOption() != null && item.getLensOption().getPrice() != null)
                        ? item.getLensOption().getPrice()
                        : BigDecimal.ZERO;

                BigDecimal unitPrice = variantPrice.add(lensPrice);
                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

                totalPrice = totalPrice.add(subtotal);
                totalItems += item.getQuantity();

                CartItemDTO itemDTO = CartItemDTO.builder()
                        .cartItemId(item.getCartItemId())
                        .variantId(item.getVariant().getVariantId())
                        .productId(item.getProductId() != null ? item.getProductId() : (item.getVariant().getProduct() != null ? item.getVariant().getProduct().getProductId() : null))
                        .isLens(item.getIsLens())
                        .isPreorder(item.getIsPreorder())
                        .productName(item.getProductName() != null ? item.getProductName() : (item.getVariant().getProduct() != null ? item.getVariant().getProduct().getName() : ""))
                        .variantColor(item.getVariant().getColor())
                        .variantSize(item.getVariant().getFrameSize())
                        .imageUrl(item.getVariant().getImageUrl())
                        .quantity(item.getQuantity())
                        .unitPrice(unitPrice)
                        .subtotal(subtotal)
                        // Mapping manual prescription fields to CartItemDTO
                        .sphLeft(item.getPrescription() != null ? item.getPrescription().getSphLeft() : null)
                        .sphRight(item.getPrescription() != null ? item.getPrescription().getSphRight() : null)
                        .cylLeft(item.getPrescription() != null ? item.getPrescription().getCylLeft() : null)
                        .cylRight(item.getPrescription() != null ? item.getPrescription().getCylRight() : null)
                        .axisLeft(item.getPrescription() != null ? item.getPrescription().getAxisLeft() : null)
                        .axisRight(item.getPrescription() != null ? item.getPrescription().getAxisRight() : null)
                        .addLeft(item.getPrescription() != null ? item.getPrescription().getAddLeft() : null)
                        .addRight(item.getPrescription() != null ? item.getPrescription().getAddRight() : null)

                        .build();

                itemDTOs.add(itemDTO);
            }
        }

        return CartDTO.builder()
                .cartId(cart.getCartId())
                .userId(cart.getUser().getUserId())
                .items(itemDTOs)
                .totalPrice(totalPrice)
                .totalItems(totalItems)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}
