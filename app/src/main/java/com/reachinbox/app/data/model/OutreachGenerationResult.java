package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OutreachGenerationResult {
    @SerializedName("subject")
    private String subject;

    @SerializedName("bodyText")
    private String bodyText;

    @SerializedName("bodyHtml")
    private String bodyHtml;

    @SerializedName("personalizationPoints")
    private List<String> personalizationPoints;

    @SerializedName("callToAction")
    private String callToAction;

    public String getSubject() { return subject; }
    public String getBodyText() { return bodyText; }
    public String getBodyHtml() { return bodyHtml; }
    public List<String> getPersonalizationPoints() { return personalizationPoints; }
    public String getCallToAction() { return callToAction; }
}
