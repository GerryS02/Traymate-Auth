package com.traymate.backend.mealOrders;

import com.traymate.backend.menu.Meal; // Add this
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors; // Add this

@RestController
@RequestMapping("/mealOrders")
@RequiredArgsConstructor
public class MealOrdersController {
    
    private final MealOrdersService mealOrdersService;
    private final MealOrdersRepository mealOrdersRepository;

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
        String message = e.getMessage();
        Object data = null;

        // If conflict, ignore the string parsing and just look it up manually
        if (message.contains("PENDING_CONFLICT")) {
            // Re-run the same search the service just did to get the object for the UI
            data = mealOrdersRepository.findByUserIdAndMealOfDayAndDate(
                newOrder.getUserId(), 
                newOrder.getMealOfDay(), 
                newOrder.getDate()
            ).orElse(null);
            
            message = "PENDING_CONFLICT"; 
        }

        ErrorResponse error = new ErrorResponse(message, "Conflict detected", data);
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }
}

    @PutMapping("/{id}")
    public ResponseEntity<MealOrders> overwriteOrder(
        @PathVariable Integer id, 
        @RequestBody MealOrders updatedOrder
    ) {
        // We use the ID from the URL to ensure we hit the right record
        MealOrders saved = mealOrdersService.updateExistingOrderById(id, updatedOrder);
        return ResponseEntity.ok(saved);
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

    //updates to change order status:
    // 1. Single Update: PUT /mealOrders/status/single?userId=22&mealOfDay=Dinner&date=2026-03-14&newStatus=preparing
    @PutMapping("/status/single")
    public ResponseEntity<MealOrders> updateStatus(
        @RequestParam String userId,
        @RequestParam String mealOfDay,
        @RequestParam String date,
        @RequestParam String newStatus,
        @RequestParam(required = false) String cook // Optional so 'ready/served' doesn't require it again
    ) {
        LocalDate localDate = LocalDate.parse(date);
        MealOrders updated = mealOrdersService.updateSingleStatus(userId, mealOfDay, localDate, newStatus, cook);
        return ResponseEntity.ok(updated);
    }

    // 2. Bulk Update: PUT /mealOrders/status/bulk?mealOfDay=Dinner&date=2026-03-14&newStatus=ready
    @PutMapping("/status/bulk")
    public ResponseEntity<String> updateBulkStatus(
        @RequestParam String mealOfDay,
        @RequestParam String date,
        @RequestParam String newStatus
    ) {
        LocalDate localDate = LocalDate.parse(date);
        mealOrdersService.updateAllStatuses(mealOfDay, localDate, newStatus);
        return ResponseEntity.ok("All " + mealOfDay + " orders updated to " + newStatus);
    }

}