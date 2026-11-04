package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class CreateJobRequest {
    @SerializedName("title")
    private String title;

    @SerializedName("company")
    private String company;

    @SerializedName("description")
    private String description;

    public CreateJobRequest(String title, String company, String description) {
        this.title = title;
        this.company = company;
        this.description = description;
    }
}
