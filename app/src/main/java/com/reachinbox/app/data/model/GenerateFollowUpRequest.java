package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class GenerateFollowUpRequest {
    @SerializedName("parentEmailId")
    private String parentEmailId;

    @SerializedName("instructions")
    private String instructions;

    @SerializedName("followUpNumber")
    private int followUpNumber;

    public GenerateFollowUpRequest(String parentEmailId, String instructions, int followUpNumber) {
        this.parentEmailId = parentEmailId;
        this.instructions = instructions;
        this.followUpNumber = followUpNumber;
    }
}
