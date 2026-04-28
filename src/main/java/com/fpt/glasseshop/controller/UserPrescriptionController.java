package com.fpt.glasseshop.controller;

import com.fpt.glasseshop.entity.UserAccount;
import com.fpt.glasseshop.entity.UserPrescription;
import com.fpt.glasseshop.repository.UserAccountRepository;
import com.fpt.glasseshop.service.UserPrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.fpt.glasseshop.entity.dto.UserPrescriptionDTO;
import java.util.List;

@RestController
@RequestMapping("/api/user-prescriptions")
@RequiredArgsConstructor
public class UserPrescriptionController {

    private final UserPrescriptionService service;
    private final UserAccountRepository userAccountRepository; // ✅ dùng

    private UserAccount getCurrentUser(Authentication auth) {
        String email = auth.getName(); // ✅ lấy email từ JWT

        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public List<UserPrescription> getMy(Authentication auth) {
        return service.getMy(getCurrentUser(auth));
    }

    @PostMapping
    public UserPrescription save(
            Authentication auth,
            @RequestBody UserPrescriptionDTO dto
    ) {
        UserAccount user = getCurrentUser(auth);

        UserPrescription data = UserPrescription.builder()
                .name(dto.getName())
                .sphLeft(dto.getSphLeft())
                .sphRight(dto.getSphRight())
                .cylLeft(dto.getCylLeft())
                .cylRight(dto.getCylRight())
                .axisLeft(dto.getAxisLeft())
                .axisRight(dto.getAxisRight())
                .addLeft(dto.getAddLeft())
                .addRight(dto.getAddRight())

                .user(user)
                .build();

        return service.save(user, data);
    }

    @DeleteMapping("/{id}")
    public void delete(Authentication auth, @PathVariable Long id) {
        service.delete(getCurrentUser(auth), id);
    }
}