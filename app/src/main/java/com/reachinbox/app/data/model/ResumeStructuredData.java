package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ResumeStructuredData {
    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("summary")
    private String summary;

    @SerializedName("skills")
    private List<String> skills;

    @SerializedName("education")
    private List<String> education;

    @SerializedName("experience")
    private List<String> experience;

    @SerializedName("projects")
    private List<String> projects;

    @SerializedName("certifications")
    private List<String> certifications;

    @SerializedName("achievements")
    private List<String> achievements;

    @SerializedName("links")
    private List<String> links;

    // Getters
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getSummary() { return summary; }
    public List<String> getSkills() { return skills; }
    public List<String> getEducation() { return education; }
    public List<String> getExperience() { return experience; }
    public List<String> getProjects() { return projects; }
    public List<String> getCertifications() { return certifications; }
    public List<String> getAchievements() { return achievements; }
    public List<String> getLinks() { return links; }
}
