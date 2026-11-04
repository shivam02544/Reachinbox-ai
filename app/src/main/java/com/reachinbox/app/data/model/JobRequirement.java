package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class JobRequirement {
    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("company")
    private String company;

    @SerializedName("description")
    private String description;

    @SerializedName("structuredData")
    private JobAnalysis structuredData;

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public String getDescription() { return description; }
    public JobAnalysis getStructuredData() { return structuredData; }
}
