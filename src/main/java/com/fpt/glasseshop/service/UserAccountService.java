package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.UserAccount;
import com.fpt.glasseshop.entity.dto.UserAccountDTO;
import com.fpt.glasseshop.entity.dto.request.UpdateProfileRequest;
import com.fpt.glasseshop.exception.ResourceNotFoundException;
import com.fpt.glasseshop.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    public boolean checkEmailExists(String email) {
        System.out.println("Checking email: " + email);
        boolean exists = userAccountRepository.existsByEmailIgnoreCase(email);
        System.out.println("Exists? " + exists);
        return exists;
    }
    public List<UserAccountDTO> getAllUsersDTO() {
        return userAccountRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<UserAccountDTO> getUserDTOById(Long id) {
        return userAccountRepository.findById(id).map(this::convertToDTO);
    }

    public UserAccountDTO createUser(UserAccountDTO dto) {
        UserAccount user = UserAccount.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .role(dto.getRole())
                .passwordHash(dto.getPassword() != null ? passwordEncoder.encode(dto.getPassword()) : null)
                .accountStatus(dto.getAccountStatus() != null ? dto.getAccountStatus() : "ACTIVE")
                .build();
        UserAccount saved = userAccountRepository.save(user);
        return convertToDTO(saved);
    }
    public Long getUserIdByEmail(String email) {
        return userAccountRepository.findByEmail(email).map(UserAccount::getUserId).orElse(null);
    }
    public void deleteUser(Long id) {
        if (!userAccountRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userAccountRepository.deleteById(id);
    }
    public String authenticate(String email, String password) {
        Optional<UserAccount> optionalUser = userAccountRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return "INVALID";
        }

        UserAccount user = optionalUser.get();

        if ("LOCKED".equalsIgnoreCase(user.getAccountStatus())) {
            return "LOCKED";
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return "INVALID";
        }

        return "SUCCESS";
    }
    private UserAccountDTO convertToDTO(UserAccount user) {
        return UserAccountDTO.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .password(user.getPasswordHash())
                .role(user.getRole())
                .accountStatus(user.getAccountStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public UserAccount getUserByEmail(String email) {
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public UserAccountDTO updateUserProfile(UpdateProfileRequest request) throws BadRequestException {
        UserAccount user = getCurrentUser();

        boolean hasAnyField =
                request.getName() != null ||
                        request.getPhone() != null ||
                        request.getEmail() != null ||
                        request.getNewPassword() != null ||
                        request.getCurrentPassword() != null ||
                        request.getConfirmPassword() != null;

        if (!hasAnyField) {
            throw new BadRequestException("At least one field must be provided for update");
        }

        // update fullName
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }

        // update phone
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            user.setPhone(request.getPhone().trim());
        }

        // update email
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String newEmail = request.getEmail().trim().toLowerCase();

            boolean exists = userAccountRepository.existsByEmail(newEmail);
            if (exists && !newEmail.equalsIgnoreCase(user.getEmail())) {
                throw new BadRequestException("Email already exists");
            }

            user.setEmail(newEmail);
        }

        // update password
        boolean wantsChangePassword =
                request.getCurrentPassword() != null ||
                        request.getNewPassword() != null ||
                        request.getConfirmPassword() != null;

        if (wantsChangePassword) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new BadRequestException("Current password is required");
            }

            if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
                throw new BadRequestException("New password is required");
            }

            if (request.getConfirmPassword() == null || request.getConfirmPassword().isBlank()) {
                throw new BadRequestException("Confirm password is required");
            }

            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new BadRequestException("Current password is incorrect");
            }

            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new BadRequestException("Confirm password does not match");
            }

            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        UserAccount saved = userAccountRepository.save(user);
        return mapToDTO(saved);
    }

    private UserAccount getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserAccountDTO mapToDTO(UserAccount user) {
        return UserAccountDTO.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();
    }

    public UserAccountDTO lockUser(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Không khóa admin acc
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Cannot lock admin account");
        }

        user.setAccountStatus("LOCKED");
        UserAccount saved = userAccountRepository.save(user);
        return mapToDTO(saved);
    }

    public UserAccountDTO unlockUser(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setAccountStatus("ACTIVE");
        return mapToDTO(userAccountRepository.save(user));
    }
}