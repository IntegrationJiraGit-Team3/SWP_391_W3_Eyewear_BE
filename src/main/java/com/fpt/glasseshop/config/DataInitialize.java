package com.fpt.glasseshop.config;

import com.fpt.glasseshop.entity.*;
import com.fpt.glasseshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class

DataInitialize implements CommandLineRunner {

        private final UserAccountRepository userAccountRepository;
        private final ProductRepository productRepository;
        private final ProductVariantRepository productVariantRepository;
        private final LensOptionRepository lensOptionRepository;
        private final PromotionRepository promotionRepository;

        private final JdbcTemplate jdbcTemplate;
        private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

        @Override
        public void run(String... args) throws Exception {
                log.info("Ensuring database schema is up-to-date...");
                try {
                        // Tự động thêm cột is_preorder vào bảng order_item nếu chưa có (H2 or MySQL
                        // syntax)
                        jdbcTemplate.execute(
                                        "ALTER TABLE order_item ADD COLUMN IF NOT EXISTS is_preorder BOOLEAN DEFAULT FALSE");
                        log.info("Database schema check: 'is_preorder' column verified.");
                } catch (Exception e) {
                        log.warn("Could not add 'is_preorder' column automatically (it might already exist). Detail: {}",
                                        e.getMessage());
                }

                log.info("Checking data initialization status...");
                if (userAccountRepository.count() == 0) {
                        log.info("Seeding users...");
                        seedUsers();
                }
                if (productRepository.count() == 0) {
                        log.info("Seeding products...");
                        seedProducts();
                }
                if (lensOptionRepository.count() == 0) {
                        log.info("Seeding lens options...");
                        seedLensOptions();
                }
                if (promotionRepository.count() == 0) {
                        log.info("Seeding promotions...");
                        seedPromotions();
                }

                log.info("Data initialization check complete. Product count: {}", productRepository.count());
        }

        private void seedUsers() {
                UserAccount admin = UserAccount.builder()
                                .name("Admin User")
                                .email("admin@example.com")
                                .phone("1234567890")
                                .role("ADMIN")
                                .passwordHash(passwordEncoder.encode("admin123"))
                                .accountStatus("ACTIVE")
                                .build();

                UserAccount staff = UserAccount.builder()
                                .name("Staff User")
                                .email("staff@example.com")
                                .phone("1122334455")
                                .role("OPERATIONAL_STAFF")
                                .passwordHash(passwordEncoder.encode("staff123"))
                                .accountStatus("ACTIVE")
                                .build();

                UserAccount customer = UserAccount.builder()
                                .name("John Doe")
                                .email("john.doe@example.com")
                                .phone("0987654321")
                                .role("CUSTOMER")
                                .passwordHash(passwordEncoder.encode("customer123"))
                                .accountStatus("ACTIVE")
                                .build();

                userAccountRepository.saveAll(Arrays.asList(admin, staff, customer));
        }

        private void seedProducts() {

                // ══════════════════════════════════════
                // FRAMES (5 products)
                // ══════════════════════════════════════

                // Frame 1: Ray-Ban Aviator Classic
                Product aviator = Product.builder()
                                .name("Ray-Ban Aviator Classic Metal")
                                .brand("Ray-Ban")
                                .description("The iconic Ray-Ban Aviator Classic features a teardrop-shaped metal frame with a double bridge. Originally designed for U.S. military pilots, this timeless style remains one of the most recognizable eyewear designs in the world.")
                                .productType(Product.ProductType.FRAME)
                                .isPrescriptionSupported(true)
                                .price(new BigDecimal("100000"))
                                .build();
                productRepository.save(aviator);
                productVariantRepository.saveAll(Arrays.asList(
                                ProductVariant.builder().product(aviator).stockQuantity(100).frameSize("Medium")
                                                .color("Gold / Green").material("Metal")
                                                .imageUrl("https://images.unsplash.com/photo-1572635196237-14b3f281503f?q=80&w=500&auto=format&fit=crop")
                                                .status("AVAILABLE").active(true).deleted(false).build(),
                                ProductVariant.builder().product(aviator).stockQuantity(100).frameSize("Large")
                                                .color("Silver / Blue").material("Metal")
                                                .imageUrl("https://images.unsplash.com/photo-1511499767150-a48a237f0083?q=80&w=500&auto=format&fit=crop")
                                                .status("AVAILABLE").active(true).deleted(false).build()));

                // Frame 2: Oakley Holbrook Square
                Product holbrook = Product.builder()
                                .name("Oakley Holbrook Square")
                                .brand("Oakley")
                                .description("The Oakley Holbrook Square blends classic retro styling with modern precision engineering. Featuring a lightweight O-Matter frame and Unobtainium earsock inserts for a secure, comfortable fit during any activity.")
                                .productType(Product.ProductType.FRAME)
                                .isPrescriptionSupported(true)
                                .price(new BigDecimal("100000"))
                                .build();
                productRepository.save(holbrook);
                productVariantRepository.saveAll(Arrays.asList(
                                ProductVariant.builder().product(holbrook).stockQuantity(100).frameSize("Large")
                                                .color("Matte Black").material("O-Matter")
                                                .imageUrl("https://images.unsplash.com/photo-1574258495973-f010dfbb5371?q=80&w=500&auto=format&fit=crop")
                                                .status("AVAILABLE").active(true).deleted(false).build(),
                                ProductVariant.builder().product(holbrook).stockQuantity(100).frameSize("Medium")
                                                .color("Polished Tortoise").material("O-Matter")
                                                .imageUrl("https://images.unsplash.com/photo-1473496169904-658ba7c44d8a?q=80&w=500&auto=format&fit=crop")
                                                .status("AVAILABLE").active(true).deleted(false).build()));

                // Frame 3: Tom Ford Tyler Round
                Product tomFordTyler = Product.builder()
                                .name("Tom Ford Tyler Round Optical")
                                .brand("Tom Ford")
                                .description("Tom Ford's Tyler optical frames embody sophisticated Italian craftsmanship. The round acetate frame with signature keyhole bridge and T-temple logo epitomizes understated luxury for the modern professional.")
                                .productType(Product.ProductType.FRAME)
                                .isPrescriptionSupported(true)
                                .price(new BigDecimal("100000"))
                                .build();
                productRepository.save(tomFordTyler);
                productVariantRepository.saveAll(Arrays.asList(
                                ProductVariant.builder().product(tomFordTyler).stockQuantity(100).frameSize("Small")
                                                .color("Shiny Black").material("Acetate")
                                                .imageUrl("https://images.unsplash.com/photo-1591076482161-42ce6da69f67?q=80&w=500&auto=format&fit=crop")
                                                .status("AVAILABLE").active(true).deleted(false).build()));

                // Frame 4: Gentle Monster Musee
                Product gentleMusee = Product.builder()
                                .name("Gentle Monster Musee Rectangle")
                                .brand("Gentle Monster")
                                .description("Inspired by architectural geometry, the Gentle Monster Musee features a sleek rectangular acetate frame with bold proportions. A favorite among fashion-forward individuals seeking a statement piece that balances edge with elegance.")
                                .productType(Product.ProductType.FRAME)
                                .isPrescriptionSupported(true)
                                .price(new BigDecimal("100000"))
                                .build();
                productRepository.save(gentleMusee);
                productVariantRepository.saveAll(Arrays.asList(
                                ProductVariant.builder().product(gentleMusee).stockQuantity(100).frameSize("Medium")
                                                .color("Ivory White").material("Acetate")
                                                .imageUrl("https://images.unsplash.com/photo-1577803645773-f96470509666?q=80&w=500&auto=format&fit=crop")
                                                .status("AVAILABLE").active(true).deleted(false).build(),
                                ProductVariant.builder().product(gentleMusee).stockQuantity(100).frameSize("Large")
                                                .color("Translucent Brown").material("Acetate")
                                                .imageUrl("https://images.unsplash.com/photo-1556015048-4d3aa10df74c?q=80&w=500&auto=format&fit=crop")
                                                .status("AVAILABLE").active(true).deleted(false).build()));

                // Frame 5: Persol PO3007V
                Product persol = Product.builder()
                                .name("Persol PO3007V Classic Oval")
                                .brand("Persol")
                                .description("Crafted in Agordo, Italy since 1917, the Persol PO3007V showcases the brand's iconic Supreme Arrow mechanism and meflecto system for unparalleled comfort. A heritage piece that has never gone out of style.")
                                .productType(Product.ProductType.FRAME)
                                .isPrescriptionSupported(true)
                                .price(new BigDecimal("100000"))
                                .build();
                productRepository.save(persol);
                productVariantRepository.saveAll(Arrays.asList(
                                ProductVariant.builder().product(persol).stockQuantity(100).frameSize("Small")
                                                .color("Havana").material("Acetate")
                                                .imageUrl("https://images.unsplash.com/photo-1509695507497-903c140c43b0?q=80&w=500&auto=format&fit=crop")
                                                .status("AVAILABLE").active(true).deleted(false).build()));

                // ══════════════════════════════════════
                // LENSES (5 products)
                // ══════════════════════════════════════

                // Lens 1: Anti-Blue Light Single Vision
                Product blueLens = Product.builder()
                                .name("Anti-Blue Light Single Vision Lens")
                                .brand("Essilor")
                                .description("Essilor's Anti-Blue Light Single Vision lenses filter harmful high-energy blue light emitted by screens, reducing eye strain and improving sleep quality. Includes anti-reflective and scratch-resistant coatings as standard.")
                                .productType(Product.ProductType.LENS)
                                .isPrescriptionSupported(true)
                                .price(new BigDecimal("200000"))
                                .build();
                productRepository.save(blueLens);
                productVariantRepository.saveAll(Arrays.asList(
                                ProductVariant.builder().product(blueLens).stockQuantity(100).color("Clear")
                                                .material("Polycarbonate 1.59")
                                                .imageUrl("https://encrypted-tbn0.gstatic.com/shopping?q=tbn:ANd9GcT-L41ct_FAUFQCroeovt2zAGACgaRoJI6VhIlhuZlXVNhQy2Yhwb2EZAFj95-2H6l80Ij_RgMedY2SFJ0vOLAev74FPItNDNQ1M0iUBFvHCzXBq-cUDi8P3QJCFPEbOOkIfP2WO4s&usqp=CAc")
                                                .status("AVAILABLE").active(true).deleted(false).build()));

                // Lens 2: High-Index 1.67 Aspheric
                Product hiIndexLens = Product.builder()
                                .name("High-Index 1.67 Aspheric Lens")
                                .brand("Hoya")
                                .description("Hoya's High-Index 1.67 Aspheric lenses are ideal for high prescriptions, offering up to 30% thinner and lighter lenses compared to standard alternatives. Superior optical clarity with multi-layered anti-reflective coating.")
                                .productType(Product.ProductType.LENS)
                                .isPrescriptionSupported(true)
                                .price(new BigDecimal("200000"))
                                .build();
                productRepository.save(hiIndexLens);
                productVariantRepository.saveAll(Arrays.asList(
                                ProductVariant.builder().product(hiIndexLens).stockQuantity(100).color("Clear")
                                                .material("Plastic 1.67")
                                                .imageUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRuLiKaDy8hfWI1g46hYUalyEkkuwr3DWHn4A&s")
                                                .status("AVAILABLE").active(true).deleted(false).build()));

                // Lens 3: Photochromic Transition
                Product transLens = Product.builder()
                                .name("Photochromic Transition Grey Lens")
                                .brand("Transitions")
                                .description("Transitions Signature GEN 8 lenses darken automatically when exposed to UV light and return to clear indoors — all within minutes. Provides 100% UV protection and reduces glare for seamless indoor-outdoor vision.")
                                .productType(Product.ProductType.LENS)
                                .isPrescriptionSupported(true)
                                .price(new BigDecimal("200000"))
                                .build();
                productRepository.save(transLens);
                productVariantRepository.saveAll(Arrays.asList(
                                ProductVariant.builder().product(transLens).stockQuantity(100).color("Transition Grey")
                                                .material("Polycarbonate 1.59")
                                                .imageUrl("https://encrypted-tbn3.gstatic.com/shopping?q=tbn:ANd9GcQdkEu2Ea0-ZAnNiLMCJFO7kYoK3CS6HoOUOHWvLb6GuEFhReHAQk3FzFV_g-zY5LToo3FHRG675Vc0VvL0haQcKYzMfnEiYWzQz3Q89iWNtKuAjU4WH4Hd1A&usqp=CAc")
                                                .status("AVAILABLE").active(true).deleted(false).build(),
                                ProductVariant.builder().product(transLens).stockQuantity(100).color("Transition Brown")
                                                .material("Polycarbonate 1.59")
                                                .imageUrl("https://encrypted-tbn3.gstatic.com/shopping?q=tbn:ANd9GcQdkEu2Ea0-ZAnNiLMCJFO7kYoK3CS6HoOUOHWvLb6GuEFhReHAQk3FzFV_g-zY5LToo3FHRG675Vc0VvL0haQcKYzMfnEiYWzQz3Q89iWNtKuAjU4WH4Hd1A&usqp=CAc")
                                                .status("AVAILABLE").active(true).deleted(false).build()));

                // Lens 4: Progressive Varifocal
                Product progressiveLens = Product.builder()
                                .name("Progressive Varifocal Premium Lens")
                                .brand("Zeiss")
                                .description("Zeiss Progressive Individual 2 lenses are the gold standard for presbyopia correction, providing seamless vision at all distances — near, intermediate, and far — in a single lens. Customized to your unique visual needs and frame choice.")
                                .productType(Product.ProductType.LENS)
                                .isPrescriptionSupported(true)
                                .price(new BigDecimal("200000"))
                                .build();
                productRepository.save(progressiveLens);
                productVariantRepository.saveAll(Arrays.asList(
                                ProductVariant.builder().product(progressiveLens).stockQuantity(100).color("Clear")
                                                .material("High-Index 1.74")
                                                .imageUrl("https://encrypted-tbn3.gstatic.com/shopping?q=tbn:ANd9GcSaXup8DqDjcWMXOpG_Lvi2nf-xCbhTNLpuiKnFtlY0Qn1fz54lReQ7W8QdQxBkkiOlv83TPgUYrFcIFEp1XaWhVd7Sh9FLrb5BmKVm_M9E&usqp=CAc")
                                                .status("AVAILABLE").active(true).deleted(false).build()));

                // Lens 5: Polarized Sunglasses Lens
                Product polarizedLens = Product.builder()
                                .name("Polarized UV400 Sunglasses Lens")
                                .brand("Oakley")
                                .description("Oakley Prizm Polarized lenses use precision-tuned wavelength filtering to boost contrast and enhance visibility on water, snow, and pavement. Provides maximum UV400 protection while eliminating 99% of blinding glare.")
                                .productType(Product.ProductType.LENS)
                                .isPrescriptionSupported(false)
                                .price(new BigDecimal("200000"))
                                .build();
                productRepository.save(polarizedLens);
                productVariantRepository.saveAll(Arrays.asList(
                                ProductVariant.builder().product(polarizedLens).stockQuantity(100)
                                                .color("Prizm Black Polarized").material("Plutonite")
                                                .imageUrl("https://encrypted-tbn0.gstatic.com/shopping?q=tbn:ANd9GcTUItyRdur9b0sIcVHhwELNgO3ranNz4WlUHQreh0Ay-mrrOYHZUQnQtAe7EHXODmpPJz2xJCnlFqDXvutNmRgtWGCP-psozS2uivlu3EjCv8xxzrPsHQNlXNZwNTKYfgxkvll6CAMDXA&usqp=CAc")
                                                .status("AVAILABLE").active(true).deleted(false).build(),
                                ProductVariant.builder().product(polarizedLens).stockQuantity(100)
                                                .color("Prizm Sapphire").material("Plutonite")
                                                .imageUrl("https://encrypted-tbn0.gstatic.com/shopping?q=tbn:ANd9GcTUItyRdur9b0sIcVHhwELNgO3ranNz4WlUHQreh0Ay-mrrOYHZUQnQtAe7EHXODmpPJz2xJCnlFqDXvutNmRgtWGCP-psozS2uivlu3EjCv8xxzrPsHQNlXNZwNTKYfgxkvll6CAMDXA&usqp=CAc")
                                                .status("AVAILABLE").active(true).deleted(false).build()));

        }

        private void seedLensOptions() {
                LensOption singleVision = LensOption.builder()
                                .type("Single Vision")
                                .thickness("1.50 Standard")
                                .coating("Anti-Reflective")
                                .color("Clear")
                                .price(new BigDecimal("300000"))
                                .build();

                LensOption highIndex = LensOption.builder()
                                .type("High Index")
                                .thickness("1.67 Thin")
                                .coating("Blue Light Filter")
                                .color("Clear")
                                .price(new BigDecimal("650000"))
                                .build();

                LensOption photochromic = LensOption.builder()
                                .type("Photochromic")
                                .thickness("1.59 Polycarbonate")
                                .coating("Scratch Resistant")
                                .color("Transition Grey")
                                .price(new BigDecimal("800000"))
                                .build();

                lensOptionRepository.saveAll(Arrays.asList(singleVision, highIndex, photochromic));
        }

        private void seedPromotions() {
                Promotion welcomePromo = Promotion.builder()
                                .code("WELCOME20")
                                .description("Welcome discount for new members")
                                .discountType(Promotion.DiscountType.PERCENTAGE)
                                .discountValue(new BigDecimal("20.00"))
                                .startDate(LocalDateTime.now())
                                .endDate(LocalDateTime.now().plusMonths(1))
                                .isActive(true)
                                .build();

                promotionRepository.save(welcomePromo);
        }

}
