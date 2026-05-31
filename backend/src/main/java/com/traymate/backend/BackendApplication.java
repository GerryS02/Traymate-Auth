package com.traymate.backend;

import com.traymate.backend.mealOrders.MealOrdersRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@SpringBootApplication
public class BackendApplication {

	private static final ZoneId FACILITY_ZONE = ZoneId.of("America/Los_Angeles");

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	// Wipe stale orders older than 7 days every time the server starts.
	// This ensures old test/March-April records never pile up in the DB
	// and prevents them from showing up on the resident or kitchen screens.
	@Bean
	@Transactional
	CommandLineRunner cleanupStaleOrders(MealOrdersRepository repo) {
		return args -> {
			LocalDate cutoff = LocalDate.now(FACILITY_ZONE).minusDays(7);
			repo.deleteByDateBefore(cutoff);
		};
	}

}
