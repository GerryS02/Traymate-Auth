package com.traymate.backend.mealOrders;

import com.traymate.backend.menu.Meal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Order payload returned to the kitchen + caregiver dashboards.
 *
 * residentName and residentRoom are denormalized onto every order
 * intentionally — the kitchen dashboard used to look these up from a
 * separate /admin/residents cache and show "—" whenever the cache
 * hadn't hydrated the matching userId yet. Embedding them here means
 * the room badge always renders correctly on the first frame.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDTO {
    private MealOrders order;
    private List<Meal> meals;
    private String residentName;
    private String residentRoom;
}