package com.fpt.glasseshop.repository;

import com.fpt.glasseshop.entity.GoogleAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GoogleAccountRepository extends JpaRepository<GoogleAccount, Long> {
    Optional<GoogleAccount> findByGmail(String gmail);
}