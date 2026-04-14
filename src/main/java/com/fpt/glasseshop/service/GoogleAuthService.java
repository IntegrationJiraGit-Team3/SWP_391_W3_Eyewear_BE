package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.GoogleAccount;
import com.fpt.glasseshop.entity.UserAccount;
import com.fpt.glasseshop.repository.GoogleAccountRepository;
import com.fpt.glasseshop.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final GoogleAccountRepository googleRepo;
    private final UserAccountRepository userRepo;

    public UserAccount handleGoogleLogin(String gmail, String name) {

        // 🔍 1. check gmail đã tồn tại chưa
        Optional<GoogleAccount> googleOpt = googleRepo.findByGmail(gmail);

        if (googleOpt.isPresent()) {
            // ✅ đã có → login
            return googleOpt.get().getUser();
        }

        // 🔍 2. check có user nào dùng gmail này chưa (edge case)
        Optional<UserAccount> userOpt = userRepo.findByEmail(gmail);

        UserAccount user;

        if (userOpt.isPresent()) {
            // 👉 user đăng ký bằng gmail trước đó
            user = userOpt.get();
        } else {
            // 👉 tạo mới user
            user = UserAccount.builder()
                    .name(name)
                    .email(gmail) // hoặc set falcon tùy bạn
                    .passwordHash("") // không cần password
                    .role("CUSTOMER")
                    .accountStatus("ACTIVE")
                    .build();

            user = userRepo.save(user);
        }

        // 🔗 3. tạo GoogleAccount
        GoogleAccount google = GoogleAccount.builder()
                .gmail(gmail)
                .user(user)
                .build();

        googleRepo.save(google);

        return user;
    }
}