package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class JobAnalysis {
    @SerializedName("jobTitle")
    private String jobTitle;

    @SerializedName("company")
    private String company;

    @SerializedName("experienceLevel")
    private String experienceLevel;

    @SerializedName("requiredSkills")
    private List<String> requiredSkills;

    @SerializedName("preferredSkills")
    private List<String> preferredSkills;

    @SerializedName("responsibilities")
    private List<String> responsibilities;

    @SerializedName("qualifications")
    private List<String> qualifications;

    @SerializedName("technologies")
    private List<String> technologies;

    @SerializedName("keywords")
    private List<String> keywords;

    public String getJobTitle() { return jobTitle; }
    public String getCompany() { return company; }
    public String getExperienceLevel() { return experienceLevel; }
    public List<String> getRequiredSkills() { return requiredSkills; }
    public List<String> getPreferredSkills() { return preferredSkills; }
    public List<String> getResponsibilities() { return responsibilities; }
    public List<String> getQualifications() { return qualifications; }
    public List<String> getTechnologies() { return technologies; }
    public List<String> getKeywords() { return keywords; }
}
