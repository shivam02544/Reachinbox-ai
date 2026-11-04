package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class CampaignStats {
    @SerializedName("total")
    private int total;
    @SerializedName("PENDING")
    private int pending;
    @SerializedName("QUEUED")
    private int queued;
    @SerializedName("PROCESSING")
    private int processing;
    @SerializedName("SENT")
    private int sent;
    @SerializedName("FAILED")
    private int failed;
    @SerializedName("CANCELLED")
    private int cancelled;

    public int getTotal() { return total; }
    public int getPending() { return pending; }
    public int getQueued() { return queued; }
    public int getProcessing() { return processing; }
    public int getSent() { return sent; }
    public int getFailed() { return failed; }
    public int getCancelled() { return cancelled; }
}
