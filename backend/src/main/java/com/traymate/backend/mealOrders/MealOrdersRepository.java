package com.traymate.backend.mealOrders;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MealOrdersRepository extends JpaRepository<MealOrders, Integer> {

    // This tells Spring: "SELECT * FROM meal_orders WHERE user_id = ?"
    List<MealOrders> findByUserId(String userId);

    // Optional: If you want to see all "pending" orders for the kitchen
    List<MealOrders> findByStatus(String status);
}