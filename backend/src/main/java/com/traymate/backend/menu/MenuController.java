package com.traymate.backend.menu;

import lombok.RequiredArgsConstructor;
//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {
    
    private final MenuService menuService;

    @GetMapping("/all")
    //@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CAREGIVER','ROLE_KITCHEN')")
    public List<Meal> getAllMeals(){
        return menuService.getAllMeals();
    }

    @GetMapping("/available")
    //@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CAREGIVER','ROLE_KITCHEN')")
    public List<Meal> getAvailableMeals(){
        return menuService.getAvailableMeals();
    }

    @GetMapping("/period/{mealperiod}")
    public List<Meal> getMealsByPeriod(@PathVariable String mealperiod) {
        return menuService.getMealsByPeriod(mealperiod);
    }
}
