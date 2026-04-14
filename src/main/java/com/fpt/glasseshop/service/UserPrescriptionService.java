package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.UserPrescription;
import com.fpt.glasseshop.entity.UserAccount;

import java.util.List;

public interface UserPrescriptionService {
    List<UserPrescription> getMy(UserAccount user);
    UserPrescription save(UserAccount user, UserPrescription data);
    void delete(UserAccount user, Long id);
}