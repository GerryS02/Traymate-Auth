package com.traymate.backend.admin.delete;

import com.traymate.backend.admin.resident.ResidentRepository;
import com.traymate.backend.auth.model.User;
import com.traymate.backend.auth.repository.UserRepository;
import com.traymate.backend.mealOrders.MealOrdersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteService {
    private final UserRepository userRepository;
    private final ResidentRepository residentRepository;
    private final MealOrdersRepository mealOrdersRepository;

    @Transactional
    public void deleteEntity(String type, Long id) {

        if (type.equalsIgnoreCase("resident")) {

            // Cascade-delete the resident's meal order history first so
            // the kitchen dashboard doesn't keep showing trays for a
            // resident who no longer exists. user_id on meal_orders is
            // VARCHAR (matches the resident id stringified) so we pass
            // it as a String.
            mealOrdersRepository.deleteByUserId(String.valueOf(id));
            residentRepository.deleteById(id.intValue());

        } else if (type.equalsIgnoreCase("user")) {

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // If caregiver, unassign from residents first
            residentRepository.findAll().forEach(resident -> {
                if (resident.getCaregiver() != null &&
                        resident.getCaregiver().getId().equals(id)) {
                    resident.setCaregiver(null);
                    residentRepository.save(resident);
                }
            });

            userRepository.delete(user);

        } else {
            throw new RuntimeException("Invalid delete type");
        }
    }
}
