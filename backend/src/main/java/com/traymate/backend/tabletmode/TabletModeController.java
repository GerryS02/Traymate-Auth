package com.traymate.backend.tabletmode;

import com.traymate.backend.admin.resident.Resident;
import com.traymate.backend.admin.resident.ResidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Tablet (kiosk) mode endpoints.
 *
 * Two pieces of state are managed here:
 *   - per-resident on/off flag (residents.tablet_mode)  — ADMIN only
 *   - facility-wide unlock PIN  (app_settings[tablet.pin]) — ADMIN + CAREGIVER
 *
 * The PIN is what staff type to unlock a locked resident dashboard, and
 * it's stored facility-wide so every tablet uses the same code. Caregivers
 * work the floor and need to view/set that PIN too, so the PIN endpoints
 * live at a neutral path (/tablet/pin) and allow both roles. The
 * per-resident toggle stays admin-only under /admin.
 *
 * The PIN is intentionally readable so the UI can show "current PIN: 1234"
 * before letting staff change it — knowledge of the PIN doesn't grant any
 * privilege a logged-in staff member doesn't already have.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TabletModeController {

    private static final String PIN_KEY = "tablet.pin";
    private static final String DEFAULT_PIN = "1234";

    private final ResidentRepository residentRepository;
    private final AppSettingRepository appSettingRepository;

    // ── Per-resident toggle ─────────────────────────────────────

    @PutMapping("/admin/residents/{id}/tablet-mode")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> setTabletMode(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> body
    ) {
        Optional<Resident> opt = residentRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        Resident r = opt.get();
        r.setTabletMode(enabled);
        residentRepository.save(r);
        log.info("[TabletMode] resident {} → {}", id, enabled ? "ON" : "OFF");
        return ResponseEntity.ok(Map.of(
            "residentId", id,
            "tabletMode", enabled
        ));
    }

    // ── Facility-wide PIN (admin + caregiver) ───────────────────
    // Lives at /tablet/pin (not /admin) so caregivers can reach it —
    // SecurityConfig restricts /admin/** to ROLE_ADMIN. Role enforcement
    // here is via @PreAuthorize; /tablet/** falls through to the
    // authenticated() catch-all in SecurityConfig.

    @GetMapping("/tablet/pin")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CAREGIVER')")
    public ResponseEntity<Map<String, String>> getPin() {
        String pin = appSettingRepository.findById(PIN_KEY)
            .map(AppSetting::getValue)
            .orElse(DEFAULT_PIN);
        return ResponseEntity.ok(Map.of("pin", pin));
    }

    @PutMapping("/tablet/pin")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CAREGIVER')")
    public ResponseEntity<Map<String, String>> setPin(@RequestBody Map<String, String> body) {
        String pin = body.get("pin");
        if (pin == null || !pin.matches("^\\d{4,6}$")) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "PIN must be 4-6 digits"
            ));
        }
        AppSetting row = appSettingRepository.findById(PIN_KEY)
            .orElseGet(() -> AppSetting.builder().key(PIN_KEY).build());
        row.setValue(pin);
        row.setUpdatedAt(OffsetDateTime.now());
        appSettingRepository.save(row);
        log.info("[TabletMode] PIN updated");
        return ResponseEntity.ok(Map.of("pin", pin));
    }
}
