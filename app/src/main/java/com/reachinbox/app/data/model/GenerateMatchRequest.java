package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class GenerateMatchRequest {
    @SerializedName("jobRequirementId")
    private String jobRequirementId;

    public GenerateMatchRequest(String jobRequirementId) {
        this.jobRequirementId = jobRequirementId;
    }
}
