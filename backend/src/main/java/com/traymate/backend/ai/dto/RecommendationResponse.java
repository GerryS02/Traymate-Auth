package com.traymate.backend.ai.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RecommendationResponse {

    private Integer residentId;
    private String residentName;

    private String foodAllergies;
    private String dietaryRestrictions;

    private String recommendation;
}