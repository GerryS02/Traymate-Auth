package com.traymate.backend.mealOrders;

import com.traymate.backend.menu.Meal; // Add this
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors; // Add this

@RestController
@RequestMapping("/mealOrders")
@RequiredArgsConstructor
public class MealOrdersController {
    
    private final MealOrdersService mealOrdersService;

    // 1. SAVE a new order
//    @PostMapping
//    public MealOrders placeOrder(@RequestBody MealOrders newOrder) {
//        return mealOrdersService.saveOrder(newOrder);
//    }

    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody MealOrders newOrder) {
      try {
          MealOrders saved = mealOrdersService.saveOrder(newOrder);
          return new ResponseEntity<>(saved, HttpStatus.CREATED);
      } catch (IllegalStateException e) {
          // Here is where the client-side "sees" the 409 and the specific code
          ErrorResponse error = new ErrorResponse(e.getMessage(), "Conflict detected", null);
          return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }
}

    // 2. RETRIEVE history for a specific user
    @GetMapping("/history/{userId}")
    public List<OrderResponseDTO> getUserHistory(@PathVariable String userId) {
        //return mealOrdersService.getUserHistory(userId);
        return mealOrdersService.getUserHistoryWithDetails(userId);
    }
    
    //3. get information for a specific meal and date
    @GetMapping("/search")
    public List<OrderResponseDTO> searchOrders(
      @RequestParam String mealOfDay, 
      @RequestParam String date // We'll receive this as a String like "2026-03-18"
    ) {
      LocalDate localDate = LocalDate.parse(date);
      return mealOrdersService.getOrdersByMealAndDate(mealOfDay, localDate);
    }

    public static record ErrorResponse(String errorCode, String message, Object data) {}
}
