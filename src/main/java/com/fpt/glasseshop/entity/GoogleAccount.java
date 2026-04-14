package com.fpt.glasseshop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "google_account")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String gmail;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserAccount user;
}
