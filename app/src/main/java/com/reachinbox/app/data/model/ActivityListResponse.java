package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ActivityListResponse {
    @SerializedName("activities")
    private List<RecentActivity> activities;

    @SerializedName("pagination")
    private EmailListResponse.Pagination pagination;

    public List<RecentActivity> getActivities() { return activities; }
    public EmailListResponse.Pagination getPagination() { return pagination; }
}
