package com.traymate.backend.admin.resident.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateResidentRequest {

    private String firstName;
    private String middleName;
    private String lastName;

    private LocalDate dob;
    private String gender;

    private String residentId;
    private String email;
    private String phone;

    private String emergencyContact;
    private String emergencyPhone;

    private String doctor;
    private String doctorPhone;

    private String medicalConditions;
    private String foodAllergies;
    private String medications;
}

