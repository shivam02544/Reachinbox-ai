package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class GenerateOutreachRequest {

    @SerializedName("jobRequirementId")
    private String jobRequirementId;

    @SerializedName("recipient")
    private Recipient recipient;

    @SerializedName("instructions")
    private String instructions;

    @SerializedName("tone")
    private String tone;

    public GenerateOutreachRequest(String jobRequirementId, Recipient recipient, String instructions, String tone) {
        this.jobRequirementId = jobRequirementId;
        this.recipient = recipient;
        this.instructions = instructions;
        this.tone = tone;
    }

    public static class Recipient {
        @SerializedName("name")
        private String name;

        @SerializedName("email")
        private String email;

        @SerializedName("company")
        private String company;

        @SerializedName("role")
        private String role;

        public Recipient(String name, String email, String company, String role) {
            this.name = name;
            this.email = email;
            this.company = company;
            this.role = role;
        }
    }
}
