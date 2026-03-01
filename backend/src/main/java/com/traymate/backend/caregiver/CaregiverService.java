package com.traymate.backend.caregiver;

import com.traymate.backend.admin.resident.Resident;
import com.traymate.backend.admin.resident.ResidentRepository;
import com.traymate.backend.auth.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CaregiverService {

    private final ResidentRepository residentRepository;

    public List<Resident> getAssignedResidents() {

        // Get logged-in user
        User caregiver = (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return residentRepository.findByCaregiver_Id(caregiver.getId());
    }
}