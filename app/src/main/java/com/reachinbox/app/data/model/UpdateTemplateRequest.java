package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class UpdateTemplateRequest {
    @SerializedName("name")
    private String name;

    @SerializedName("subject")
    private String subject;

    @SerializedName("bodyText")
    private String bodyText;

    @SerializedName("bodyHtml")
    private String bodyHtml;

    public void setName(String name) { this.name = name; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setBodyText(String bodyText) { this.bodyText = bodyText; }
    public void setBodyHtml(String bodyHtml) { this.bodyHtml = bodyHtml; }
}
