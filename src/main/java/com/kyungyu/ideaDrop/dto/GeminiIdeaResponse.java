package com.kyungyu.ideaDrop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiIdeaResponse(
        TypeAGlobal typeAGlobal,
        TypeBDomestic typeBDomestic
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TypeAGlobal(
            String ideaName,
            String coreSummary,
            String trendAnalysis,
            String kSuccessPoint,
            String globalMarketGap,
            String coreTech,
            String riskAssessment,
            String firstStep
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TypeBDomestic(
            String ideaName,
            String coreSummary,
            String trendAnalysis,
            String painPointSolution,
            String businessModel,
            String pmKick,
            String firstStep
    ) {}
}