package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DashboardData {
    @SerializedName("stats")
    private DashboardStats stats;

    @SerializedName("recentActivity")
    private List<RecentActivity> recentActivity;

    public DashboardStats getStats() { return stats; }
    public List<RecentActivity> getRecentActivity() { return recentActivity; }
}
