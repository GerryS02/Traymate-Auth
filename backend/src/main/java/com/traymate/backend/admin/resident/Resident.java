package com.traymate.backend.admin.resident;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// @Entity
// @Table(name = "residents")
// @Getter
// @Setter
// @Builder
// @NoArgsConstructor
// @AllArgsConstructor
// public class Resident {

//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;

//     private String firstName;
//     private String middleName;
//     private String lastName;

//     private LocalDate dob;
//     private String gender;

//     @Column(unique = true)
//     private String residentId;

//     @Column(unique = true)
//     private String email;

//     private String phone;

//     private String emergencyContact;
//     private String emergencyPhone;

//     private String doctor;
//     private String doctorPhone;

//     @Column(length = 1000)
//     private String medicalConditions;

//     @Column(length = 1000)
//     private String foodAllergies;

//     @Column(length = 1000)
//     private String medications;
// }

@Entity
@Table(name = "residents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resident {

    // PRIMARY KEY — matches DB
    @Id
    @Column(name = "resident_id")
    private String residentId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "gender")
    private String gender;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @Column(name = "emergency_phone")
    private String emergencyPhone;

    @Column(name = "doctor")
    private String doctor;

    @Column(name = "doctor_phone")
    private String doctorPhone;

    @Column(name = "medical_conditions")
    private String medicalConditions;

    @Column(name = "food_allergies")
    private String foodAllergies;

    @Column(name = "medications")
    private String medications;
}
