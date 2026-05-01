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

    public MealOrders saveOrder(MealOrders order) {
        if (order.getDate() == null) {
            order.setDate(LocalDate.now());
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

    // --- Detail Hydration Methods ---

    public List<OrderResponseDTO> getUserHistoryWithDetails(String userId) {
        List<MealOrders> orders = mealOrdersRepository.findByUserId(userId);
        return orders.stream().map(order -> {
            List<Meal> meals = getDetailedMealsForOrder(order.getMealItemsIdNumbers());
            return new OrderResponseDTO(order, meals);
        }).collect(Collectors.toList());
    }

    public List<OrderResponseDTO> getOrdersByMealAndDate(String mealOfDay, LocalDate date) {
        List<MealOrders> orders = mealOrdersRepository.findByMealOfDayAndDate(mealOfDay, date);
        return orders.stream().map(order -> {
            List<Meal> meals = getDetailedMealsForOrder(order.getMealItemsIdNumbers());
            return new OrderResponseDTO(order, meals);
        }).collect(Collectors.toList());
    }

    public List<Meal> getDetailedMealsForOrder(String mealItemsIdNumbers) {
        if (mealItemsIdNumbers == null || mealItemsIdNumbers.isEmpty()) return List.of();
        List<Integer> ids = Arrays.stream(mealItemsIdNumbers.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        return mealRepository.findAllById(ids);
    }
}