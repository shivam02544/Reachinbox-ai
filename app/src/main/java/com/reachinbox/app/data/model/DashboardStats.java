package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class DashboardStats {
    @SerializedName("totalEmails")
    private int totalEmails;

    @SerializedName("drafts")
    private int drafts;

    @SerializedName("scheduled")
    private int scheduled;

    @SerializedName("sent")
    private int sent;

    @SerializedName("failed")
    private int failed;

    @SerializedName("totalSenders")
    private int totalSenders;

    public int getTotalEmails() { return totalEmails; }
    public int getDrafts() { return drafts; }
    public int getScheduled() { return scheduled; }
    public int getSent() { return sent; }
    public int getFailed() { return failed; }
    public int getTotalSenders() { return totalSenders; }
}
