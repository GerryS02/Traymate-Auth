package com.traymate.backend.menu;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Write-side menu endpoints used by the kitchen "Add Meal" flow and the
 * admin Manage Menu screen. Lives next to MenuController so it shares the
 * same /menu namespace — that's important because /admin/** is locked to
 * ROLE_ADMIN in SecurityConfig, and kitchen staff need to be able to add
 * and remove meals from the kitchen dashboard.
 *
 * Each route is method-gated via @PreAuthorize so we get fine-grained
 * role control (admin + kitchen + kitchen_staff) without weakening the
 * /admin/** lockdown.
 */
@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuMutationController {

    private final MealRepository mealRepository;

    /** Create a new meal. Returns 201 with the saved meal (id populated). */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_KITCHEN_STAFF','ROLE_KITCHEN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Meal create(@RequestBody MealCreateRequest request) {
        Meal meal = new Meal();

        // Force-clear the id so callers can't overwrite an existing row by
        // sending an id in the body. New rows always start with id=null and
        // let JPA assign one via IDENTITY.
        meal.setId(null);
        meal.setName(defaultString(request.name()));
        meal.setIngredients(defaultString(request.ingredients()));
        meal.setDescription(defaultString(request.description()));
        meal.setImageUrl(defaultString(request.imageUrl()));
        meal.setMealtype(request.mealtype());
        meal.setMealperiod(defaultString(request.mealperiod()));
        meal.setTimeRange(defaultString(request.timeRange()));
        meal.setAllergenInfo(defaultString(request.allergenInfo()));
        meal.setTags(defaultString(request.tags()));
        meal.setAvailable(Boolean.TRUE.equals(request.available()));
        meal.setSeasonal(Boolean.TRUE.equals(request.seasonal()));
        meal.setNutrition(defaultString(request.nutrition()));
        meal.setCalories(request.calories());
        meal.setSodium(request.sodium());
        meal.setProtein(request.protein());

        return mealRepository.save(meal);
    }

    /**
     * Update an existing meal. PUT semantics: the body fully replaces the
     * existing record. Returns 404 if the id doesn't exist (instead of
     * silently inserting a new row, which would mask client bugs).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_KITCHEN_STAFF','ROLE_KITCHEN')")
    public Meal update(@PathVariable Integer id, @RequestBody Meal incoming) {
        Meal existing = mealRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meal not found"));

        // Copy mutable fields over the existing row instead of overwriting
        // the whole entity reference. Keeps JPA's dirty tracking happy and
        // avoids accidentally nuking server-only fields if any get added later.
        existing.setName(incoming.getName());
        existing.setIngredients(incoming.getIngredients());
        existing.setDescription(incoming.getDescription());
        existing.setImageUrl(incoming.getImageUrl());
        existing.setMealtype(incoming.getMealtype());
        existing.setMealperiod(incoming.getMealperiod());
        existing.setTimeRange(incoming.getTimeRange());
        existing.setAllergenInfo(incoming.getAllergenInfo());
        existing.setTags(incoming.getTags());
        existing.setNameTranslations(incoming.getNameTranslations());
        existing.setDescriptionTranslations(incoming.getDescriptionTranslations());
        existing.setTagTranslations(incoming.getTagTranslations());
        existing.setAvailable(incoming.isAvailable());
        existing.setSeasonal(incoming.isSeasonal());
        existing.setNutrition(incoming.getNutrition());
        existing.setCalories(incoming.getCalories());
        existing.setSodium(incoming.getSodium());
        existing.setProtein(incoming.getProtein());

        return mealRepository.save(existing);
    }

    /**
     * Patch only persisted translation JSON. Used by the kitchen add-meal
     * flow after Gemini translates the newly-created row; avoids a partial
     * PUT accidentally clearing core menu fields.
     */
    @PatchMapping("/{id}/translations")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_KITCHEN_STAFF','ROLE_KITCHEN')")
    public Meal updateTranslations(@PathVariable Integer id, @RequestBody Meal incoming) {
        Meal existing = mealRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meal not found"));

        existing.setNameTranslations(incoming.getNameTranslations());
        existing.setDescriptionTranslations(incoming.getDescriptionTranslations());
        existing.setTagTranslations(incoming.getTagTranslations());

        return mealRepository.save(existing);
    }

    /** Delete a meal. Returns 204 on success, 404 if it didn't exist. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_KITCHEN_STAFF','ROLE_KITCHEN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        if (!mealRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Meal not found");
        }
        mealRepository.deleteById(id);
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    public record MealCreateRequest(
        String name,
        String ingredients,
        String description,
        String imageUrl,
        String mealtype,
        String mealperiod,
        String timeRange,
        String allergenInfo,
        String tags,
        Boolean available,
        Boolean seasonal,
        String nutrition,
        Integer calories,
        Integer sodium,
        Integer protein
    ) {}
}
