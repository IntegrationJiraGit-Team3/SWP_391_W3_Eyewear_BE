package com.fpt.glasseshop.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "address")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserAccount user;
    @Column(columnDefinition = "NVARCHAR(255)")
    private String street;
    @Column(columnDefinition = "NVARCHAR(100)")
    private String city;
    @Column(columnDefinition = "NVARCHAR(100)")
    private String state;
    private String zipCode;
    @Column(columnDefinition = "NVARCHAR(100)")
    private String country;
}
