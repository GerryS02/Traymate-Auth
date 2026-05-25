// package com.traymate.backend.ai;

// import java.util.List;
// import java.util.Map;
// import java.util.Set;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.HttpStatusCode;
// import org.springframework.http.MediaType;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;
// import org.springframework.web.client.RestClient;
// import org.springframework.web.client.RestClientResponseException;

// import com.traymate.backend.admin.resident.Resident;
// import com.traymate.backend.menu.MealRepository;
// import com.traymate.backend.admin.resident.ResidentRepository;
// import com.traymate.backend.ai.dto.ChatRequest;
// import com.traymate.backend.ai.dto.ChatResponse;
// import com.traymate.backend.ai.dto.RecommendationRequest;
// import com.traymate.backend.ai.dto.RecommendationResponse;
// import com.traymate.backend.menu.Meal;

// import lombok.RequiredArgsConstructor;

// @RestController
// @RequestMapping("/ai")
// @RequiredArgsConstructor
// public class AiController {

//     private final ResidentRepository residentRepository;
//     private final MealRepository mealRepository;

//     private static final Set<String> ALLOWED_MODELS = Set.of(
//         "gemini-2.5-flash",
//         "gemini-2.0-flash",
//         "gemini-2.0-flash-lite"
//     );

//     private static final String UPSTREAM_BASE =
//         "https://generativelanguage.googleapis.com/v1beta/models";

//     @Value("${GEMINI_API_KEY:}")
//     private String geminiApiKey;

//     // private final RestClient http = RestClient.builder().build();
//     private final RestClient http = RestClient.create();

//     /*
//      * =========================================================
//      * GENERIC GEMINI PROXY
//      * =========================================================
//      */

//     // @PostMapping("/gemini")
//     // public ResponseEntity<String> proxy(
//     //         @RequestBody Map<String, Object> payload) {

//     //     if (geminiApiKey == null || geminiApiKey.isBlank()) {
//     //         return error(500, "GEMINI_API_KEY env var not set on server");
//     //     }

//     //     Object modelObj = payload == null ? null : payload.get("model");
//     //     Object bodyObj  = payload == null ? null : payload.get("body");

//     //     if (!(modelObj instanceof String) || bodyObj == null) {
//     //         return error(
//     //             400,
//     //             "Expected JSON body { model: <string>, body: <object> }"
//     //         );
//     //     }

//     //     String model = (String) modelObj;

//     //     if (!ALLOWED_MODELS.contains(model)) {
//     //         return error(400, "Model not allowed: " + model);
//     //     }

//     //     String url =
//     //         UPSTREAM_BASE + "/" + model +
//     //         ":generateContent?key=" + geminiApiKey;

//     //     try {

//     //         String upstreamBody = http.post()
//     //             .uri(url)
//     //             .contentType(MediaType.APPLICATION_JSON)
//     //             .body(bodyObj)
//     //             .retrieve()
//     //             .body(String.class);

//     //         return ResponseEntity.ok()
//     //             .contentType(MediaType.APPLICATION_JSON)
//     //             .body(upstreamBody);

//     //     } catch (RestClientResponseException ex) {

//     //         return ResponseEntity.status(ex.getStatusCode())
//     //             .contentType(MediaType.APPLICATION_JSON)
//     //             .body(ex.getResponseBodyAsString());

//     //     } catch (Exception ex) {

//     //         return error(502, "Upstream fetch failed: " + ex.getMessage());
//     //     }
//     // }
//     @PostMapping("")
//     public ResponseEntity<ChatResponse> chat(
//             @RequestBody ChatRequest req) {

//         try {

//             if (req.getMessage() == null || req.getMessage().isBlank()) {

//                 return ResponseEntity.badRequest().body(
//                         ChatResponse.builder()
//                                 .response("Message is required")
//                                 .build()
//                 );
//             }

//             String prompt = req.getMessage();

//             Map<String, Object> body = Map.of(
//                     "contents", List.of(
//                             Map.of(
//                                     "parts", List.of(
//                                             Map.of("text", prompt)
//                                     )
//                             )
//                     )
//             );

//             String url =
//                     UPSTREAM_BASE +
//                     "/gemini-2.5-flash:generateContent?key=" +
//                     geminiApiKey;

//             Map response = http.post()
//                     .uri(url)
//                     .contentType(MediaType.APPLICATION_JSON)
//                     .body(body)
//                     .retrieve()
//                     .body(Map.class);

//             List candidates = (List) response.get("candidates");

//             Map firstCandidate = (Map) candidates.get(0);

//             Map content = (Map) firstCandidate.get("content");

//             List parts = (List) content.get("parts");

//             Map firstPart = (Map) parts.get(0);

//             String aiText = (String) firstPart.get("text");

//             return ResponseEntity.ok(
//                     ChatResponse.builder()
//                             .response(aiText)
//                             .build()
//             );

//         } catch (Exception ex) {

//             ex.printStackTrace();

//             return ResponseEntity.internalServerError().body(
//                     ChatResponse.builder()
//                             .response(
//                                     "AI request failed: "
//                                             + ex.getMessage()
//                             )
//                             .build()
//             );
//         }
//     }

//     /*
//      * =========================================================
//      * ERROR RESPONSE
//      * =========================================================
//      */

//     private static ResponseEntity<String> error(
//             int status,
//             String message) {

//         String json =
//             "{\"error\":{\"message\":\"" +
//             message.replace("\"", "\\\"") +
//             "\"}}";

//         return ResponseEntity.status(HttpStatusCode.valueOf(status))
//             .contentType(MediaType.APPLICATION_JSON)
//             .body(json);
//     }

//     @PostMapping("/recommendation")
//     public ResponseEntity<RecommendationResponse> getRecommendation(
//             @RequestBody RecommendationRequest req) {

//         try {

//             Resident resident = residentRepository
//                     .findById(req.getResidentId())
//                     .orElseThrow(() ->
//                             new RuntimeException("Resident not found"));

//             String allergies =
//                     resident.getFoodAllergies() == null
//                             ? ""
//                             : resident.getFoodAllergies().toLowerCase();

//             String restrictions =
//                     resident.getDietaryRestrictions() == null
//                             ? ""
//                             : resident.getDietaryRestrictions();

//             /*
//             * LOAD AVAILABLE MEALS
//             */
//             List<Meal> meals = mealRepository.findByAvailableTrue();

//             /*
//             * FILTER OUT ALLERGENS
//             */
//             List<Meal> safeMeals = meals.stream()

//                     .filter(meal -> {

//                         String allergenInfo =
//                                 meal.getAllergenInfo() == null
//                                         ? ""
//                                         : meal.getAllergenInfo().toLowerCase();

//                         String ingredients =
//                                 meal.getIngredients() == null
//                                         ? ""
//                                         : meal.getIngredients().toLowerCase();

//                         /*
//                         * VERY SIMPLE ALLERGY FILTER
//                         */
//                         if (!allergies.isBlank()) {

//                             String[] allergyList = allergies.split(",");

//                             for (String allergy : allergyList) {

//                                 String trimmed = allergy.trim();

//                                 if (allergenInfo.contains(trimmed)
//                                         || ingredients.contains(trimmed)) {

//                                     return false;
//                                 }
//                             }
//                         }

//                         return true;
//                     })

//                     .limit(15)
//                     .toList();

//             /*
//             * BUILD SAFE MEAL LIST
//             */
//             StringBuilder mealText = new StringBuilder();

//             for (Meal meal : safeMeals) {

//                 mealText.append("""
//                     Meal: %s
//                     Description: %s
//                     Ingredients: %s
//                     Tags: %s

//                     """.formatted(
//                         meal.getName(),
//                         meal.getDescription(),
//                         meal.getIngredients(),
//                         meal.getTags()
//                 ));
//             }

//             /*
//             * GEMINI PROMPT
//             */
//             String prompt = """
//                 You are a meal recommendation assistant for elderly residents.

//                 Resident dietary restrictions:
//                 %s

//                 User request:
//                 %s

//                 ONLY recommend meals from this safe list:

//                 %s

//                 Rules:
//                 - Recommend at most 3 meals
//                 - Keep responses concise
//                 - Explain briefly why each meal fits
//                 - NEVER mention meals outside the provided list
//                 """
//                 .formatted(
//                     restrictions,
//                     req.getQuestion(),
//                     mealText
//                 );

//             Map<String, Object> body = Map.of(
//                 "contents", java.util.List.of(
//                     Map.of(
//                         "parts", java.util.List.of(
//                             Map.of("text", prompt)
//                         )
//                     )
//                 )
//             );

//             String url =
//                     UPSTREAM_BASE +
//                     "/gemini-2.5-flash:generateContent?key=" +
//                     geminiApiKey;

//             // String geminiResponse = http.post()
//             //         .uri(url)
//             //         .contentType(MediaType.APPLICATION_JSON)
//             //         .body(body)
//             //         .retrieve()
//             //         .body(String.class);
//             Map response = http.post()
//             .uri(url)
//             .contentType(MediaType.APPLICATION_JSON)
//             .body(body)
//             .retrieve()
//             .body(Map.class);

//             List candidates = (List) response.get("candidates");
//             Map firstCandidate = (Map) candidates.get(0);
//             Map content = (Map) firstCandidate.get("content");
//             List parts = (List) content.get("parts");
//             Map firstPart = (Map) parts.get(0);
//             String aiText = (String) firstPart.get("text");

//             return ResponseEntity.ok(
//                     RecommendationResponse.builder()
//                             .residentId(resident.getId())
//                             .residentName(
//                                     resident.getFirstName()
//                                             + " "
//                                             + resident.getLastName()
//                             )
//                             .allergies(allergies)
//                             .dietaryRestrictions(restrictions)
//                             .recommendation(aiText)
//                             .build()
//             );

//         } catch (Exception ex) {

//             ex.printStackTrace();

//             return ResponseEntity.internalServerError().body(
//                     RecommendationResponse.builder()
//                             .recommendation(
//                                     "Recommendation failed: "
//                                             + ex.getMessage()
//                             )
//                             .build()
//             );
//         }
//     }
// }

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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final ResidentRepository residentRepository;
    private final MealRepository mealRepository;

    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    private final RestClient http = RestClient.create();

    /*
     * =========================================================
     * SINGLE AI ENDPOINT
     * =========================================================
     */

    @PostMapping
    public ResponseEntity<ChatResponse> handleAi(@RequestBody ChatRequest req) {

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

                String aiText = callGemini(prompt);

                return ResponseEntity.ok(
                        new ChatResponse(aiText)
                );
            }

            /*
             * =====================================================
             * 2. NORMAL CHAT FLOW
             * =====================================================
             */

            String prompt = req.getMessage();

            String aiText = callGemini(prompt);

            return ResponseEntity.ok(
                    new ChatResponse(aiText)
            );

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("Error: " + ex.getMessage()));
        }
    }

    /*
     * =========================================================
     * GEMINI CALL WRAPPER
     * =========================================================
     */

    private String callGemini(String prompt) {

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );

        String url =
                "https://generativelanguage.googleapis.com/v1beta/models/" +
                        "gemini-2.5-flash:generateContent?key=" +
                        geminiApiKey;

        Map response = http.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        List candidates = (List) response.get("candidates");

        Map first = (Map) candidates.get(0);
        Map content = (Map) first.get("content");
        List parts = (List) content.get("parts");

        return (String) ((Map) parts.get(0)).get("text");
    }
}