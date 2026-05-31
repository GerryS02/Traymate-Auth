package com.traymate.backend.mealOrders;

import com.traymate.backend.admin.resident.Resident;
import com.traymate.backend.admin.resident.ResidentRepository;
import com.traymate.backend.compliance.DietaryComplianceService;
import com.traymate.backend.compliance.dto.ComplianceResult;
import com.traymate.backend.menu.Meal;
import com.traymate.backend.menu.MealRepository;
import com.traymate.backend.override.MedicalOverrideRequest;
import com.traymate.backend.override.MedicalOverrideService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealOrdersService {

    private final MealOrdersRepository mealOrdersRepository;
    private final MealRepository mealRepository;
    private final ResidentRepository residentRepository;
    private final DietaryComplianceService complianceService;
    private final MedicalOverrideService overrideService;

    // Facility-local timezone used when the client doesn't send a date.
    // Render hosts run in UTC, so plain LocalDate.now() returns TOMORROW
    // any time after ~5 PM Pacific. Seattle is Pacific Time (same IANA
    // zone), so dinner orders placed at 6:30 PM saved as the NEXT day
    // and disappeared from the kitchen's "today" view. Hard-coded for
    // the showcase; promote to env var (`FACILITY_TZ`) if multi-site.
    private static final ZoneId FACILITY_ZONE = ZoneId.of("America/Los_Angeles");

    public MealOrders saveOrder(MealOrders order) {
        if (order.getDate() == null) {
            order.setDate(LocalDate.now(FACILITY_ZONE));
        }

        // 1. Check for existing order conflicts
        Optional<MealOrders> existingOrder = mealOrdersRepository.findByUserIdAndMealOfDayAndDate(
            order.getUserId(), 
            order.getMealOfDay(), 
            order.getDate()
        );

        if (existingOrder.isPresent()) {
            String currentStatus = existingOrder.get().getStatus();
            
            if ("pending".equalsIgnoreCase(currentStatus)) {
                throw new IllegalStateException("PENDING_CONFLICT");
            }
            
            if ("preparing".equalsIgnoreCase(currentStatus) || "completed".equalsIgnoreCase(currentStatus)) {
                throw new IllegalStateException("LOCKED_STATUS");
            }
        }

        // 2. Dietary Compliance Gate
        enforceCompliance(order);

        order.setStatus("pending");
        return mealOrdersRepository.save(order);
    }

    public MealOrders updateExistingOrderById(Integer id, MealOrders newOrderData) {
        MealOrders existing = mealOrdersRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Order ID " + id + " no longer exists"));

        if (!"pending".equalsIgnoreCase(existing.getStatus())) {
            throw new IllegalStateException("LOCKED_STATUS");
        }

        existing.setMealItemsIdNumbers(newOrderData.getMealItemsIdNumbers());

        // Re-run compliance check for the updated items
        enforceCompliance(existing);

        return mealOrdersRepository.save(existing);
    }

    /**
     * Safety Gate: Validates meals against resident dietary profiles and checks for admin overrides.
     */
    private void enforceCompliance(MealOrders order) {
        if (order.getUserId() == null || order.getMealItemsIdNumbers() == null) return;

        Integer residentId;
        try {
            residentId = Integer.parseInt(order.getUserId().trim());
        } catch (NumberFormatException e) {
            return; 
        }

        Optional<Resident> residentOpt = residentRepository.findById(residentId);
        if (residentOpt.isEmpty()) return;

        List<Integer> mealIds;
        try {
            mealIds = Arrays.stream(order.getMealItemsIdNumbers().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            return;
        }
        if (mealIds.isEmpty()) return;

        List<Meal> meals = mealRepository.findAllById(mealIds);
        ComplianceResult result = complianceService.validate(residentOpt.get(), meals);
        
        if (result.isSafe()) return;

        // Check for an approved Medical Override
        Optional<MedicalOverrideRequest> activeOverride = overrideService.findActiveApproval(
            residentId, order.getMealOfDay(), order.getDate(), mealIds
        );

        if (activeOverride.isPresent()) {
            overrideService.consume(activeOverride.get());
            return;
        }

        throw new ComplianceBlockedException(result);
    }

    // --- Status & Management Methods (Restored from your Core) ---

    public MealOrders updateSingleStatus(String userId, String mealOfDay, LocalDate date, String newStatus, String cook) {
        MealOrders order = mealOrdersRepository.findByUserIdAndMealOfDayAndDate(userId, mealOfDay, date)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        order.setStatus(newStatus.toLowerCase());
        if ("preparing".equalsIgnoreCase(newStatus)) {
            order.setCook(cook);
        }
        return mealOrdersRepository.save(order);
    }

    public void updateAllStatuses(String mealOfDay, LocalDate date, String newStatus, String cook) {
        List<MealOrders> orders = mealOrdersRepository.findByMealOfDayAndDate(mealOfDay, date);
        orders.forEach(order -> {
            order.setStatus(newStatus.toLowerCase());
            if ("preparing".equalsIgnoreCase(newStatus)) {
                order.setCook(cook);
            }
        });
        mealOrdersRepository.saveAll(orders);
    }

    @Transactional
    public void deleteOrder(String userId, String mealOfDay, LocalDate date) {
        MealOrders order = mealOrdersRepository.findByUserIdAndMealOfDayAndDate(userId, mealOfDay, date)
              .orElseThrow(() -> new RuntimeException("Order not found"));
        mealOrdersRepository.delete(order);
    }

    /** Hard-delete by primary key — used when the frontend only has the order ID. */
    @Transactional
    public void deleteOrderById(Integer id) {
        if (!mealOrdersRepository.existsById(id)) {
            throw new RuntimeException("Order ID " + id + " not found");
        }
        mealOrdersRepository.deleteById(id);
    }

    // --- Detail Hydration Methods ---

    public List<OrderResponseDTO> getUserHistoryWithDetails(String userId) {
        // Only return orders from the last 14 days. Returning all-time history
        // causes old cancelled/completed orders to persist visually on the
        // resident screen after they should have cleared, and bloats the
        // in-memory CartContext on long-running sessions.
        LocalDate cutoff = LocalDate.now(FACILITY_ZONE).minusDays(14);
        List<MealOrders> orders = mealOrdersRepository.findByUserIdAndDateGreaterThanEqual(userId, cutoff);
        return orders.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<OrderResponseDTO> getOrdersByMealAndDate(String mealOfDay, LocalDate date) {
        List<MealOrders> orders = mealOrdersRepository.findByMealOfDayAndDate(mealOfDay, date);
        return orders.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Build the wire DTO including the resident's display name + room.
     * Embedding these on every order means the kitchen dashboard's room
     * badge renders on the first frame instead of showing "—" until a
     * separate /admin/residents call resolves the user. Falls through
     * silently for orders whose userId isn't a valid resident id (legacy
     * data, deleted residents) — the DTO just gets nulls and the UI
     * keeps its existing fallback chain.
     */
    private OrderResponseDTO toResponse(MealOrders order) {
        List<Meal> meals = getDetailedMealsForOrder(order.getMealItemsIdNumbers());
        String residentName = null;
        String residentRoom = null;
        try {
            if (order.getUserId() != null && !order.getUserId().isBlank()) {
                int rid = Integer.parseInt(order.getUserId().trim());
                Optional<Resident> opt = residentRepository.findById(rid);
                if (opt.isPresent()) {
                    Resident r = opt.get();
                    residentName = formatResidentName(r);
                    residentRoom = r.getRoomNumber();
                }
            }
        } catch (NumberFormatException ignored) {
            // userId isn't a numeric resident id — leave name/room null
        }
        return new OrderResponseDTO(order, meals, residentName, residentRoom);
    }

    private String formatResidentName(Resident r) {
        StringBuilder sb = new StringBuilder();
        if (r.getFirstName() != null && !r.getFirstName().isBlank()) sb.append(r.getFirstName().trim());
        if (r.getMiddleName() != null && !r.getMiddleName().isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(r.getMiddleName().trim());
        }
        if (r.getLastName() != null && !r.getLastName().isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(r.getLastName().trim());
        }
        String name = sb.toString();
        return name.isEmpty() ? null : name;
    }

    public List<Meal> getDetailedMealsForOrder(String mealItemsIdNumbers) {
        if (mealItemsIdNumbers == null || mealItemsIdNumbers.isEmpty()) return List.of();
        List<Integer> ids = Arrays.stream(mealItemsIdNumbers.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        return mealRepository.findAllById(ids);
    }
    
    public String getMostFrequentOrder(String userId) {
    List<MealOrders> history = mealOrdersRepository.findByUserId(userId);

    if (history.isEmpty()) {
        return null; // No history to base a recommendation on
    }

    // 1. Create HashMap: Map<MealString, Frequency>
    java.util.Map<String, Integer> frequencyMap = new java.util.HashMap<>();

    for (MealOrders order : history) {
        String items = order.getMealItemsIdNumbers();
        if (items != null && !items.isEmpty()) {
            frequencyMap.put(items, frequencyMap.getOrDefault(items, 0) + 1);
        }
    }

    // 2. Find the meal string with the highest frequency
    return frequencyMap.entrySet().stream()
            .max(java.util.Map.Entry.comparingByValue())
            .map(java.util.Map.Entry::getKey)
            .orElse(null);
}
}