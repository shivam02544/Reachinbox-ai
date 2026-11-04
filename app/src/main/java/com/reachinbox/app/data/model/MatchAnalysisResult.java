package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MatchAnalysisResult {
    @SerializedName("matchingSkills")
    private List<String> matchingSkills;

    @SerializedName("missingSkills")
    private List<String> missingSkills;

    @SerializedName("relevantExperience")
    private List<String> relevantExperience;

    @SerializedName("relevantProjects")
    private List<String> relevantProjects;

    @SerializedName("matchingQualifications")
    private List<String> matchingQualifications;

    @SerializedName("potentialGaps")
    private List<String> potentialGaps;

    @SerializedName("personalizationPoints")
    private List<String> personalizationPoints;

    public List<String> getMatchingSkills() { return matchingSkills; }
    public List<String> getMissingSkills() { return missingSkills; }
    public List<String> getRelevantExperience() { return relevantExperience; }
    public List<String> getRelevantProjects() { return relevantProjects; }
    public List<String> getMatchingQualifications() { return matchingQualifications; }
    public List<String> getPotentialGaps() { return potentialGaps; }
    public List<String> getPersonalizationPoints() { return personalizationPoints; }
}
