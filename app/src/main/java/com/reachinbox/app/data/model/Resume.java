package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class Resume {
    @SerializedName("id")
    private String id;

    @SerializedName("userId")
    private String userId;

    @SerializedName("fileName")
    private String fileName;

    @SerializedName("status")
    private String status;

    @SerializedName("structuredData")
    private ResumeStructuredData structuredData;

    @SerializedName("createdAt")
    private String createdAt;

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getFileName() { return fileName; }
    public String getStatus() { return status; }
    public ResumeStructuredData getStructuredData() { return structuredData; }
    public String getCreatedAt() { return createdAt; }
}
