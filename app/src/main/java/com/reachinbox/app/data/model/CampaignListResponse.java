package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CampaignListResponse {
    @SerializedName("campaigns")
    private List<Campaign> campaigns;
    @SerializedName("pagination")
    private Pagination pagination;

    public List<Campaign> getCampaigns() { return campaigns; }
    public Pagination getPagination() { return pagination; }

    public static class Pagination {
        @SerializedName("total")
        private int total;
        @SerializedName("page")
        private int page;
        @SerializedName("limit")
        private int limit;
        @SerializedName("totalPages")
        private int totalPages;

        public int getTotal() { return total; }
        public int getPage() { return page; }
        public int getLimit() { return limit; }
        public int getTotalPages() { return totalPages; }
    }
}
