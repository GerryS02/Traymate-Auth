package com.traymate.backend.ai;

import com.traymate.backend.admin.resident.Resident;
import com.traymate.backend.admin.resident.ResidentRepository;
import com.traymate.backend.ai.dto.ChatRequest;
import com.traymate.backend.ai.dto.ChatResponse;
import com.traymate.backend.menu.Meal;
import com.traymate.backend.menu.MealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final ResidentRepository residentRepository;
    private final MealRepository mealRepository;

    // Set GEMINI_API_KEY in the Render dashboard (Environment tab) — never commit
    // the literal key to git. If the env var is missing, callGemini() returns a
    // friendly fallback message instead of NPE-ing into a 500.
    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    private final RestClient http = RestClient.create();

    private static final String GEMINI_BASE =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    /*
     * =========================================================
     * SINGLE AI ENDPOINT  (POST /ai)
     * =========================================================
     */

    @PostMapping
        public ResponseEntity<?> handleAi(@RequestBody ChatRequest req) {

        try {
            if (req.getMessage() == null || req.getMessage().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ChatResponse("Message cannot be empty"));
            }

            String message = req.getMessage().toLowerCase();

            boolean isFoodRequest =
                    message.contains("eat") ||
                    message.contains("meal") ||
                    message.contains("food") ||
                    message.contains("breakfast") ||
                    message.contains("lunch") ||
                    message.contains("dinner") ||
                    message.contains("recommend");

            /*
             * =====================================================
             * 1. FOOD / MEAL RECOMMENDATION FLOW
             * =====================================================
             */
            if (isFoodRequest && req.getResidentId() != null) {

                Resident resident = residentRepository.findById(req.getResidentId())
                        .orElseThrow(() -> new RuntimeException("Resident not found"));

                String allergies = resident.getFoodAllergies() == null
                        ? ""
                        : resident.getFoodAllergies().toLowerCase();

                List<Meal> meals = mealRepository.findByAvailableTrue();

                List<Meal> safeMeals = meals.stream()
                        .filter(meal -> {

                            String ingredients = meal.getIngredients() == null
                                    ? ""
                                    : meal.getIngredients().toLowerCase();

                            String allergenInfo = meal.getAllergenInfo() == null
                                    ? ""
                                    : meal.getAllergenInfo().toLowerCase();

                            if (!allergies.isBlank()) {
                                for (String allergy : allergies.split(",")) {
                                    String a = allergy.trim();
                                    if (ingredients.contains(a) || allergenInfo.contains(a)) {
                                        return false;
                                    }
                                }
                            }

                            return true;
                        })
                        .limit(10)
                        .toList();

                StringBuilder mealText = new StringBuilder();

                for (Meal m : safeMeals) {
                    mealText.append("""
                            Meal: %s
                            Description: %s
                            Ingredients: %s
                            Tags: %s

                            """.formatted(
                            m.getName(),
                            m.getDescription(),
                            m.getIngredients(),
                            m.getTags()
                    ));
                }

                String prompt = """
                        You are a meal assistant for elderly residents.

                        ONLY recommend meals from this list:

                        %s

                        User request:
                        %s

                        Rules:
                        - Recommend max 3 meals
                        - Explain briefly why each is suitable
                        - Do NOT mention outside meals
                        """.formatted(mealText, req.getMessage());

                String aiText = callGemini("gemini-2.5-flash-lite", prompt);

                return ResponseEntity.ok(new ChatResponse(aiText));
            }

            /*
             * =====================================================
             * 2. NORMAL CHAT FLOW
             * =====================================================
             */

            String aiText = callGemini("gemini-2.5-flash-lite", req.getMessage());

            return ResponseEntity.ok(new ChatResponse(aiText));

        } catch (RestClientResponseException ex) {
            // Return Gemini's actual error payload so Postman shows the real cause.
            return ResponseEntity.status(ex.getStatusCode())
                    .body(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("Error: " + ex.getMessage()));
        }
    }

    /*
     * =========================================================
     * GEMINI PROXY  (POST /ai/gemini)
     * ---------------------------------------------------------
     * The mobile client's primary AI path. It sends the full
     * Gemini request as { "model": "...", "body": { ... } } and
     * expects the RAW Gemini :generateContent JSON back so it can
     * read candidates[0].content.parts itself.
     *
     * We deliberately forward Gemini's real HTTP status (e.g. 429
     * quota, 503 overload) instead of masking everything as 500,
     * because the client applies per-model cooldown logic based on
     * that status.
     * =========================================================
     */
    @PostMapping("/gemini")
    public ResponseEntity<?> handleGeminiProxy(@RequestBody Map<String, Object> req) {

        Object modelObj = req.get("model");
        String model = (modelObj == null || modelObj.toString().isBlank())
                ? "gemini-2.5-flash-lite"
                : modelObj.toString();

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) req.get("body");
        if (body == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", Map.of("message", "Missing 'body' in request")));
        }

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", Map.of("message", "GEMINI_API_KEY not configured on server")));
        }

        String url = GEMINI_BASE + model + ":generateContent?key=" + geminiApiKey;

        try {
            Map<?, ?> response = http.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return ResponseEntity.ok(response);

        } catch (RestClientResponseException ex) {
            // Forward Gemini's actual status + JSON error body verbatim so the
            // client can distinguish 429 (quota) / 503 (overload) / 404 (model).
            return ResponseEntity.status(ex.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", Map.of("message",
                            "Proxy error contacting Gemini: " + ex.getMessage())));
        }
    }

    /*
     * =========================================================
     * GEMINI CALL WRAPPER (used by the /ai chat + recommend flow)
     * =========================================================
     */

    private String callGemini(String model, String prompt) {

        System.out.println("Using Gemini model: " + model);

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return "The assistant isn't configured yet. Please try again later.";
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        String url = GEMINI_BASE + model + ":generateContent?key=" + geminiApiKey;

        Map<?, ?> response;
        try {
            response = http.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException ex) {
            // Log full response body and status to help diagnose 400/4xx errors
            System.err.println("[Gemini] HTTP " + ex.getStatusCode() + " response: " + ex.getResponseBodyAsString());
            throw ex;
        }

        // Defensive parsing — never let a malformed/blocked response NPE into a
        // bare 500. Walk the standard shape and fall back to a friendly message.
        if (response == null) return "Sorry, I couldn't reach the assistant right now.";

        Object candidatesObj = response.get("candidates");
        if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty()) {
            return "Sorry, I couldn't come up with a response right now.";
        }

        Object firstObj = candidates.get(0);
        if (!(firstObj instanceof Map<?, ?> first)) {
            return "Sorry, I couldn't come up with a response right now.";
        }

        Object contentObj = first.get("content");
        if (!(contentObj instanceof Map<?, ?> content)) {
            return "Sorry, I couldn't come up with a response right now.";
        }

        Object partsObj = content.get("parts");
        if (!(partsObj instanceof List<?> parts) || parts.isEmpty()) {
            return "Sorry, I couldn't come up with a response right now.";
        }

        Object partObj = parts.get(0);
        if (partObj instanceof Map<?, ?> part && part.get("text") != null) {
            return part.get("text").toString();
        }

        return "Sorry, I couldn't come up with a response right now.";
    }
}
