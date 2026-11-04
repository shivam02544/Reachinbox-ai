package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AddRecipientsRequest {
    @SerializedName("recipients")
    private List<String> recipients;

    public AddRecipientsRequest(List<String> recipients) {
        this.recipients = recipients;
    }

    public List<String> getRecipients() { return recipients; }
    public void setRecipients(List<String> recipients) { this.recipients = recipients; }
}
