// package com.traymate.backend.admin.assignment;

// public class AdminAssignmentService {
    
// }

package com.traymate.backend.admin.assignment;

import com.traymate.backend.admin.assignment.dto.AssignmentStatsDto;
import com.traymate.backend.admin.resident.Resident;
import com.traymate.backend.admin.resident.ResidentRepository;
import com.traymate.backend.auth.model.User;
import com.traymate.backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAssignmentService {

    private final ResidentRepository residentRepository;
    private final UserRepository userRepository;

    public void assignResident(Integer residentId, Long caregiverId) {

        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new RuntimeException("Resident not found"));

        if (caregiverId == null) {
            resident.setCaregiver(null);
        } else {
            User caregiver = userRepository.findById(caregiverId)
                    .orElseThrow(() -> new RuntimeException("Caregiver not found"));

            if (!"ROLE_CAREGIVER".equals(caregiver.getRole())) {
                throw new RuntimeException("User is not a caregiver");
            }

            resident.setCaregiver(caregiver);
        }

        residentRepository.save(resident);
    }

    public AssignmentStatsDto getStats() {
        return new AssignmentStatsDto(
                residentRepository.count(),
                residentRepository.countByCaregiverIsNotNull(),
                residentRepository.countByCaregiverIsNull()
        );
    }
}
