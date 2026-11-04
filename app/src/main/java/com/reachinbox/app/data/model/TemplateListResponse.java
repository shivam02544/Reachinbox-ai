package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TemplateListResponse {
    @SerializedName("templates")
    private List<Template> templates;

    @SerializedName("pagination")
    private Pagination pagination;

    public List<Template> getTemplates() { return templates; }
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

        public int getTotal() { return total; }
        public int getPage() { return page; }
        public int getLimit() { return limit; }
        public int getTotalPages() { return totalPages; }
        public boolean hasNextPage() { return hasNextPage; }
    }
}
