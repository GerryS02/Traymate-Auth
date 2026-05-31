package com.traymate.backend;

import com.traymate.backend.mealOrders.MealOrdersService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.time.ZoneId;

@SpringBootApplication
public class BackendApplication {

	private static final ZoneId FACILITY_ZONE = ZoneId.of("America/Los_Angeles");

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	// Wipe stale orders older than 7 days every time the server starts.
	// Routed through MealOrdersService so the delete runs inside its own
	// @Transactional context. Wrapped in try-catch so a DB hiccup on boot
	// never prevents the server from starting.
	@Bean
	CommandLineRunner cleanupStaleOrders(MealOrdersService mealOrdersService) {
		return args -> {
			try {
				LocalDate cutoff = LocalDate.now(FACILITY_ZONE).minusDays(7);
				mealOrdersService.deleteOrdersBefore(cutoff);
			} catch (Exception e) {
				System.err.println("[Startup] Stale order cleanup failed (non-fatal): " + e.getMessage());
			}
		};
	}

}
