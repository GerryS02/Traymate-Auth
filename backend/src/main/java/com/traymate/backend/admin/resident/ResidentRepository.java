package com.traymate.backend.admin.resident;

import org.springframework.data.jpa.repository.JpaRepository;

//import java.util.Optional;

public interface ResidentRepository extends JpaRepository<Resident, Integer> {

   // Optional<Resident> findByResidentId(String residentId);

    //Optional<Resident> findByEmail(String email);
}

