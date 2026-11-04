package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ScheduledEmailListResponse {
    @SerializedName("scheduledEmails")
    private List<ScheduledEmail> scheduledEmails;

    @SerializedName("pagination")
    private EmailListResponse.Pagination pagination;

    public List<ScheduledEmail> getScheduledEmails() { return scheduledEmails; }
    public EmailListResponse.Pagination getPagination() { return pagination; }
}
