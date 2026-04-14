package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.Prescription;
import com.fpt.glasseshop.entity.UserAccount;
import com.fpt.glasseshop.entity.dto.PrescriptionDTO;
import com.fpt.glasseshop.exception.ResourceNotFoundException;
import com.fpt.glasseshop.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<PrescriptionDTO> getPrescriptionsByUser(UserAccount user) {
        return prescriptionRepository.findByUserUserId(user.getUserId()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public PrescriptionDTO savePrescription(UserAccount user, PrescriptionDTO dto) {
        Prescription prescription = Prescription.builder()
                .user(user)
                .name(dto.getName())
                .sphLeft(dto.getSphLeft())
                .sphRight(dto.getSphRight())
                .cylLeft(dto.getCylLeft())
                .cylRight(dto.getCylRight())
                .axisLeft(dto.getAxisLeft())
                .axisRight(dto.getAxisRight())
                .addLeft(dto.getAddLeft())
                .addRight(dto.getAddRight())

                .doctorName(dto.getDoctorName())
                .expirationDate(dto.getExpirationDate())
                .status(true) 
                .build();
        
        Prescription saved = prescriptionRepository.save(prescription);

        notificationService.notifyAdmins(
            "New Prescription Uploaded",
            "Customer " + user.getName() + " has uploaded a new prescription.",
            "PRESCRIPTION",
            saved.getPrescriptionId()
        );
        
        return convertToDTO(saved);
    }

    @Transactional
    public void deletePrescription(UserAccount user, Long prescriptionId) {
        Prescription p = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));
        
        if (p.getUser() == null || !p.getUser().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Unauthorized delete");
        }
        
        prescriptionRepository.delete(p);
    }

    @Transactional
    public PrescriptionDTO updatePrescriptionStatus(Long prescriptionId, Boolean status, String adminNote) {
        Prescription p = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with id: " + prescriptionId));
        
        p.setStatus(status);
        p.setAdminNote(adminNote);

        UserAccount user = p.getUser();
        if (user == null && p.getOrderItem() != null && p.getOrderItem().getOrder() != null) {
            user = p.getOrderItem().getOrder().getUser();
        }

        String result = Boolean.TRUE.equals(status) ? "Approved" : "Rejected";
        if (user != null) {
            notificationService.createNotification(
                user,
                "Prescription " + result,
                "Your prescription " + (p.getName() != null ? p.getName() : "") + " has been " + result.toLowerCase() + ".",
                "PRESCRIPTION",
                p.getPrescriptionId()
            );
        }
        
        return convertToDTO(prescriptionRepository.save(p));
    }

    private PrescriptionDTO convertToDTO(Prescription p) {
        return PrescriptionDTO.builder()
                .prescriptionId(p.getPrescriptionId())
                .name(p.getName())
                .sphLeft(p.getSphLeft())
                .sphRight(p.getSphRight())
                .cylLeft(p.getCylLeft())
                .cylRight(p.getCylRight())
                .axisLeft(p.getAxisLeft())
                .axisRight(p.getAxisRight())
                .addLeft(p.getAddLeft())
                .addRight(p.getAddRight())

                .doctorName(p.getDoctorName())
                .expirationDate(p.getExpirationDate())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .adminNote(p.getAdminNote())
                .build();
    }
}
