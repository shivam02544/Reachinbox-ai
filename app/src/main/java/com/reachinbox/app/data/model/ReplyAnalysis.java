package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class ReplyAnalysis {
    @SerializedName("classification")
    private String classification;

    @SerializedName("summary")
    private String summary;

    @SerializedName("suggestedAction")
    private String suggestedAction;

    @SerializedName("confidence")
    private String confidence;

    @SerializedName("suggestedResponse")
    private SuggestedResponse suggestedResponse;

    public String getClassification() { return classification; }
    public String getSummary() { return summary; }
    public String getSuggestedAction() { return suggestedAction; }
    public String getConfidence() { return confidence; }
    public SuggestedResponse getSuggestedResponse() { return suggestedResponse; }

    public static class SuggestedResponse {
        @SerializedName("subject")
        private String subject;

        @SerializedName("bodyText")
        private String bodyText;

        public String getSubject() { return subject; }
        public String getBodyText() { return bodyText; }
    }
}
