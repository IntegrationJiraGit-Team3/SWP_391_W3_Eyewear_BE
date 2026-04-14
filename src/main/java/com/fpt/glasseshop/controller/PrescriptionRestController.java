package com.fpt.glasseshop.controller;

import com.fpt.glasseshop.entity.UserAccount;
import com.fpt.glasseshop.entity.dto.PrescriptionDTO;
import com.fpt.glasseshop.repository.UserAccountRepository;
import com.fpt.glasseshop.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionRestController {

    private final PrescriptionService prescriptionService;
    private final UserAccountRepository userAccountRepository;

    private UserAccount getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public ResponseEntity<List<PrescriptionDTO>> getMyPrescriptions() {
        UserAccount user = getCurrentUser();
        return ResponseEntity.ok(prescriptionService.getPrescriptionsByUser(user));
    }

    @PostMapping
    public ResponseEntity<PrescriptionDTO> savePrescription(@RequestBody PrescriptionDTO dto) {
        UserAccount user = getCurrentUser();
        return ResponseEntity.ok(prescriptionService.savePrescription(user, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePrescription(@PathVariable Long id) {
        UserAccount user = getCurrentUser();
        prescriptionService.deletePrescription(user, id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PrescriptionDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam Boolean status,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(prescriptionService.updatePrescriptionStatus(id, status, note));
    }
}
