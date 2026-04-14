package com.fpt.glasseshop.service.impl;

import com.fpt.glasseshop.entity.UserAccount;
import com.fpt.glasseshop.entity.UserPrescription;
import com.fpt.glasseshop.repository.UserPrescriptionRepository;
import com.fpt.glasseshop.service.UserPrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserPrescriptionServiceImpl implements UserPrescriptionService {

    private final UserPrescriptionRepository repo;

    @Override
    public List<UserPrescription> getMy(UserAccount user) {
        return repo.findByUserUserId(user.getUserId());
    }

    @Override
    public UserPrescription save(UserAccount user, UserPrescription data) {

        List<UserPrescription> list = repo.findByUserUserId(user.getUserId());

        boolean exists = list.stream().anyMatch(p ->
                Objects.equals(p.getSphLeft(), data.getSphLeft()) &&
                        Objects.equals(p.getSphRight(), data.getSphRight())
        );

        if (exists) {
            return list.stream().filter(p ->
                    Objects.equals(p.getSphLeft(), data.getSphLeft()) &&
                            Objects.equals(p.getSphRight(), data.getSphRight())
            ).findFirst().get();
        }

        data.setUser(user); // đảm bảo chắc chắn
        return repo.save(data);
    }

    @Override
    public void delete(UserAccount user, Long id) {
        UserPrescription p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        if (!p.getUser().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Unauthorized");
        }

        repo.delete(p);
    }
}