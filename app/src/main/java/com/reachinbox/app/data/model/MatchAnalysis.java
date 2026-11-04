package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class MatchAnalysis {
    @SerializedName("id")
    private String id;

    @SerializedName("userId")
    private String userId;

    @SerializedName("jobRequirementId")
    private String jobRequirementId;

    @SerializedName("result")
    private MatchAnalysisResult result;

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getJobRequirementId() { return jobRequirementId; }
    public MatchAnalysisResult getResult() { return result; }
}
