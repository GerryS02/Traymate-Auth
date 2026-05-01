package com.traymate.backend.mealOrders;

import com.traymate.backend.menu.Meal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/mealOrders")
@RequiredArgsConstructor
public class MealOrdersController {
    
    private final MealOrdersService mealOrdersService;
    private final MealOrdersRepository mealOrdersRepository;

    // 1. SAVE a new order
    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody MealOrders newOrder) {
        try {
            MealOrders saved = mealOrdersService.saveOrder(newOrder);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (ComplianceBlockedException e) {
            // New addition: Handle dietary/allergen violations
            ErrorResponse error = new ErrorResponse(
                "COMPLIANCE_BLOCKED",
                "Order contains meals that violate the resident's dietary profile",
                e.getResult()
            );
            return new ResponseEntity<>(error, HttpStatus.UNPROCESSABLE_ENTITY);
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            Object data = null;

            if (message.contains("PENDING_CONFLICT")) {
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

    // 2. UPDATE existing order
    @PutMapping("/{id}")
    public ResponseEntity<?> overwriteOrder(
        @PathVariable Integer id, 
        @RequestBody MealOrders updatedOrder
    ) {
        try {
            MealOrders saved = mealOrdersService.updateExistingOrderById(id, updatedOrder);
            return ResponseEntity.ok(saved);
        } catch (ComplianceBlockedException e) {
            // New addition: Compliance check on updates too
            ErrorResponse error = new ErrorResponse(
                "COMPLIANCE_BLOCKED",
                "Order contains meals that violate the resident's dietary profile",
                e.getResult()
            );
            return new ResponseEntity<>(error, HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    // 3. RETRIEVE history for a specific user
    @GetMapping("/history/{userId}")
    public List<OrderResponseDTO> getUserHistory(@PathVariable String userId) {
        return mealOrdersService.getUserHistoryWithDetails(userId);
    }
    
    // 4. SEARCH for a specific meal and date
    @GetMapping("/search")
    public List<OrderResponseDTO> searchOrders(
      @RequestParam String mealOfDay, 
      @RequestParam String date 
    ) {
      LocalDate localDate = LocalDate.parse(date);
      return mealOrdersService.getOrdersByMealAndDate(mealOfDay, localDate);
    }

    // 5. UPDATE Status (Single)
    @PutMapping("/status/single")
    public ResponseEntity<MealOrders> updateStatus(
        @RequestParam String userId,
        @RequestParam String mealOfDay,
        @RequestParam String date,
        @RequestParam String newStatus,
        @RequestParam(required = false) String cook
    ) {
        LocalDate localDate = LocalDate.parse(date);
        MealOrders updated = mealOrdersService.updateSingleStatus(userId, mealOfDay, localDate, newStatus, cook);
        return ResponseEntity.ok(updated);
    }

    // 6. UPDATE Status (Bulk)
    @PutMapping("/status/bulk")
    public ResponseEntity<String> updateBulkStatus(
        @RequestParam String mealOfDay,
        @RequestParam String date,
        @RequestParam String newStatus,
        @RequestParam(required = false) String cook
    ) {
        LocalDate localDate = LocalDate.parse(date);
        mealOrdersService.updateAllStatuses(mealOfDay, localDate, newStatus, cook);
        
        return ResponseEntity.ok("All " + mealOfDay + " orders updated to " + newStatus + 
                                 (cook != null ? " by " + cook : ""));
    }
    
    // 7. DELETE order
    @DeleteMapping("/remove")
    public ResponseEntity<String> deleteOrder(
        @RequestParam String userId,
        @RequestParam String mealOfDay,
        @RequestParam String date
    ) {
        LocalDate localDate = LocalDate.parse(date);
        mealOrdersService.deleteOrder(userId, mealOfDay, localDate);
        return ResponseEntity.ok("Order removed.");
    }

    public static record ErrorResponse(String errorCode, String message, Object data) {}
}