package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class Template {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("subject")
    private String subject;

    @SerializedName("bodyText")
    private String bodyText;

    @SerializedName("bodyHtml")
    private String bodyHtml;

    @SerializedName("createdAt")
    private Date createdAt;

    @SerializedName("updatedAt")
    private Date updatedAt;

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getSubject() { return subject; }
    public String getBodyText() { return bodyText; }
    public String getBodyHtml() { return bodyHtml; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }

    // Setters (for local usage if needed)
    public void setName(String name) { this.name = name; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setBodyText(String bodyText) { this.bodyText = bodyText; }
    public void setBodyHtml(String bodyHtml) { this.bodyHtml = bodyHtml; }
}
