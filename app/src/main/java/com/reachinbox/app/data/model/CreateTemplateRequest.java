package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class CreateTemplateRequest {
    @SerializedName("name")
    private String name;

    @SerializedName("subject")
    private String subject;

    @SerializedName("bodyText")
    private String bodyText;

    @SerializedName("bodyHtml")
    private String bodyHtml;

    public CreateTemplateRequest(String name, String subject, String bodyText) {
        this.name = name;
        this.subject = subject;
        this.bodyText = bodyText;
    }

    public void setBodyHtml(String bodyHtml) { this.bodyHtml = bodyHtml; }
}
