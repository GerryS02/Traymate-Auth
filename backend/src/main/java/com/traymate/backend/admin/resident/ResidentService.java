package com.traymate.backend.admin.resident;

import com.traymate.backend.admin.resident.dto.CreateResidentRequest;
import com.traymate.backend.auth.exception.AuthException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResidentService {

    private final ResidentRepository repository;

    public Resident createResident(CreateResidentRequest req) {

        // basic validation
        // if (repository.findByResidentId(req.getResidentId()).isPresent()) {
        //     throw new AuthException("Resident ID already exists");
        // }

        // if (req.getEmail() != null &&
        //     repository.findByEmail(req.getEmail()).isPresent()) {
        //     throw new AuthException("Resident email already exists");
        // }

        Resident resident = Resident.builder()
                .firstName(req.getFirstName())
                .middleName(req.getMiddleName())
                .lastName(req.getLastName())
                .dob(req.getDob())
                .gender(req.getGender())
                //.residentId(req.getResidentId())
                //.email(req.getEmail())
                .phone(req.getPhone())
                .emergencyContact(req.getEmergencyContact())
                .emergencyPhone(req.getEmergencyPhone())
                .doctor(req.getDoctor())
                .doctorPhone(req.getDoctorPhone())
                .medicalConditions(req.getMedicalConditions())
                .foodAllergies(req.getFoodAllergies())
                .medications(req.getMedications())
                .build();

        return repository.save(resident);
    }
}
