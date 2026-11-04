package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class EmailListResponse {
    @SerializedName(value = "emails", alternate = {"drafts"})
    private List<Email> emails;

    @SerializedName("pagination")
    private Pagination pagination;

    public List<Email> getEmails() { return emails; }
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
        @SerializedName("hasNextPage")
        private boolean hasNextPage;
        @SerializedName("hasPreviousPage")
        private boolean hasPreviousPage;

        public int getTotal() { return total; }
        public int getPage() { return page; }
        public int getLimit() { return limit; }
        public int getTotalPages() { return totalPages; }
        public boolean hasNextPage() { return hasNextPage; }
        public boolean hasPreviousPage() { return hasPreviousPage; }
    }
}
