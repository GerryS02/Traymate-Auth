package com.traymate.backend.mealOrders;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/mealOrders") //end point name
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
    public List<MealOrders> getHistory(@PathVariable String userId) {
        return mealOrdersService.getUserHistory(userId);
    }

}
