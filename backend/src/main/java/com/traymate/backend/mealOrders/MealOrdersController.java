package com.traymate.backend.mealOrders;

import com.traymate.backend.menu.Meal; // Add this
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors; // Add this

@RestController
@RequestMapping("/mealOrders")
@RequiredArgsConstructor
public class MealOrdersController {
    
    private final MealOrdersService mealOrdersService;

    // 1. SAVE a new order
    @PostMapping
    public MealOrders placeOrder(@RequestBody MealOrders newOrder) {
        return mealOrdersService.saveOrder(newOrder);
    }

    // 2. RETRIEVE history for a specific user
@GetMapping("/history/{userId}")
public List<OrderResponseDTO> getUserHistory(@PathVariable String userId) {
    // 1. Get the orders for just this user
    List<MealOrders> orders = mealOrdersService.getUserHistory(userId);
    
    // 2. Map them to a response that includes the meal details
    return orders.stream().map(order -> {
        List<Meal> details = mealOrdersService.getDetailedMealsForOrder(order.getMealItemsIdNumbers());
        return new OrderResponseDTO(order, details);
    }).collect(Collectors.toList());
}
}
