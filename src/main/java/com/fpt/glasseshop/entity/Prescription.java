package com.fpt.glasseshop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "prescription")
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long prescriptionId;

    @OneToOne
    @JoinColumn(name = "order_item_id", unique = true)
    private OrderItem orderItem;

    @OneToOne
    @JoinColumn(name = "cart_item_id", unique = true)
    private CartItem cartItem;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserAccount user;
    
    private String name;

    private BigDecimal sphLeft;
    private BigDecimal sphRight;
    private BigDecimal cylLeft;
    private BigDecimal cylRight;
    private Integer axisLeft;
    private Integer axisRight;
    private BigDecimal addLeft;
    private BigDecimal addRight;

    private String doctorName;
    private LocalDate expirationDate;
    
    // status = false (pending), status = true (approved)
    @Builder.Default
    private Boolean status = false;
    
    @Builder.Default
    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote = "";

    @CreationTimestamp
    private LocalDateTime createdAt;
}
