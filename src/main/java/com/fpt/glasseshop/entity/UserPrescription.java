package com.fpt.glasseshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_prescription")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPrescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnore
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


    @CreationTimestamp
    private LocalDateTime createdAt;
}