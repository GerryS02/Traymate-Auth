package com.traymate.backend.mealOrders;

import com.traymate.backend.menu.Meal;
import com.traymate.backend.menu.MealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealOrdersService {

    private final MealOrdersRepository mealOrdersRepository;
    private final MealRepository mealRepository; // Inject the existing menu repository

    public MealOrders saveOrder(MealOrders order) {
        order.setDate(LocalDate.now());
        order.setStatus("pending");
        return mealOrdersRepository.save(order);
    }

    public List<MealOrders> getUserHistory(String userId) {
        return mealOrdersRepository.findByUserId(userId);
    }

    // THIS IS THE KEY: Look up full meal details for an order
    public List<Meal> getDetailedMealsForOrder(String mealItemsIdNumbers) {
        if (mealItemsIdNumbers == null || mealItemsIdNumbers.isEmpty()) {
            return List.of();
        }

        // 1. Split "101,102" into ["101", "102"]
        List<Integer> ids = Arrays.stream(mealItemsIdNumbers.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        // 2. Query the 'meals' table for all those IDs at once
        return mealRepository.findAllById(ids);
    }
    
    public List<OrderResponseDTO> getUserHistoryWithDetails(String userId) {
    // 1. Get the list of orders for the user
    List<MealOrders> orders = mealOrdersRepository.findByUserId(userId);

    // 2. Transform each order into a DTO that includes the full Meal objects
    return orders.stream().map(order -> {
        // Use your existing "Key" function to get the list of Meals
        List<Meal> meals = getDetailedMealsForOrder(order.getMealItemsIdNumbers());
        
        // Return the combined object
        return new OrderResponseDTO(order, meals);
    }).collect(Collectors.toList());
  }
  
  // Inside MealOrdersService
public List<OrderResponseDTO> getOrdersByMealAndDate(String mealOfDay, LocalDate date) {
    // 1. Find the raw orders from the DB
    List<MealOrders> orders = mealOrdersRepository.findByMealOfDayAndDate(mealOfDay, date);

    // 2. Hydrate them into DTOs
    return orders.stream().map(order -> {
        List<Meal> meals = getDetailedMealsForOrder(order.getMealItemsIdNumbers());
        return new OrderResponseDTO(order, meals);
    }).collect(Collectors.toList());
}
  
}