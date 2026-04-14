package com.fpt.glasseshop.controller;

import com.fpt.glasseshop.entity.UserAccount;
import com.fpt.glasseshop.entity.dto.ApiResponse;
import com.fpt.glasseshop.entity.dto.UserAccountDTO;
import com.fpt.glasseshop.entity.dto.request.UpdateProfileRequest;
import com.fpt.glasseshop.service.GoogleAuthService;
import com.fpt.glasseshop.service.UserAccountService;
import com.fpt.glasseshop.security.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "UserAPI", description = "Operations related to user accounts")
public class UserAccountRestController {

    private final GoogleAuthService googleAuthService;
    private final UserAccountService userAccountService;
    private final JwtUtil jwtUtil; // ✅ FIX: inject JwtUtil

    // ================= GET ALL USERS =================
    @GetMapping("/getAllUsers")
    @Operation(summary = "Get all users", description = "Retrieves a list of all user accounts (Admin only)")
    public ResponseEntity<ApiResponse<List<UserAccountDTO>>> getAllUsers() {
        List<UserAccountDTO> users = userAccountService.getAllUsersDTO();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    // ================= GET USER BY ID =================
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserAccountDTO>> getUserById(
            @Parameter(description = "User ID") @PathVariable Long id) {

        return userAccountService.getUserDTOById(id)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)))
                .orElse(ResponseEntity.status(404).body(ApiResponse.error("User not found")));
    }

    // ================= CHECK EMAIL =================
    @PostMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {
        boolean exists = userAccountService.checkEmailExists(email);
        return ResponseEntity.ok(ApiResponse.success(exists));
    }

    // ================= CREATE USER =================
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<UserAccountDTO>> createUser(
            @RequestBody UserAccountDTO userDTO) {

        UserAccountDTO created = userAccountService.createUser(userDTO);
        return ResponseEntity.status(201)
                .body(ApiResponse.success("User created successfully", created));
    }

    // ================= LOGIN =================
    @PostMapping("/login")
    @Operation(summary = "Login and get JWT token")
    public ResponseEntity<ApiResponse<String>> login(
            @RequestParam String email,
            @RequestParam String password) {

        String result = userAccountService.authenticate(email, password);

        if ("LOCKED".equals(result)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Tài khoản của bạn đã bị khóa"));
        }

        if (!"SUCCESS".equals(result)) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Sai email hoặc mật khẩu"));
        }

        UserAccount user = userAccountService.getUserByEmail(email);

        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole());

        return ResponseEntity.ok(
                ApiResponse.success("Login successful", token)
        );
    }

    // ================= LOGIN GOOGLE =================
    @PostMapping("/login-google")
    @Operation(summary = "Login with Google and get JWT token")
    public ResponseEntity<ApiResponse<String>> loginGoogle(
            @RequestParam String gmail,
            @RequestParam String name) {

        UserAccount user = googleAuthService.handleGoogleLogin(gmail, name);

        String token = jwtUtil.generateToken(
                user.getUserId(),
                user.getEmail(),
                user.getRole()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Login Google successful", token)
        );
    }


    // ================= DELETE USER =================
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        try {
            userAccountService.deleteUser(id);
            return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ================= UPDATE PROFILE =================
    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<UserAccountDTO>> updateMyProfile(
            @RequestBody UpdateProfileRequest request) throws BadRequestException {

        UserAccountDTO updated = userAccountService.updateUserProfile(request);

        return ResponseEntity.ok(
                ApiResponse.success("User profile updated successfully", updated)
        );
    }

    @PatchMapping("/{id}/lock/")
    public ResponseEntity<ApiResponse<UserAccountDTO>> lockUser(@PathVariable Long id) {

        UserAccountDTO updated = userAccountService.lockUser(id);

        return ResponseEntity.ok(
                ApiResponse.success("User account locked successfully", updated)
        );
    }
    @PatchMapping("/{id}/unlock/")
    public ResponseEntity<ApiResponse<UserAccountDTO>> unlockUser (@PathVariable Long id) {
        UserAccountDTO updated = userAccountService.unlockUser(id);
        return ResponseEntity.ok(
                ApiResponse.success("User account unlocked successfully", updated));
    }

}