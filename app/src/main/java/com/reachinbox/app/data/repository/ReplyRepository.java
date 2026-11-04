package com.reachinbox.app.data.repository;

import android.content.Context;
import androidx.annotation.NonNull;

import com.reachinbox.app.data.api.ApiClient;
import com.reachinbox.app.data.api.ReplyApi;
import com.reachinbox.app.data.model.ReplyAnalysis;
import com.reachinbox.app.data.model.ReplyAnalysisResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReplyRepository {

    private final ReplyApi replyApi;

    public ReplyRepository(Context context) {
        this.replyApi = ApiClient.getClient(context).create(ReplyApi.class);
    }

    public interface ReplyCallback {
        void onSuccess(ReplyAnalysis result);
        void onError(String message);
    }

    public void analyzeReply(String inboundEmailId, ReplyCallback callback) {
        replyApi.analyzeReply(inboundEmailId).enqueue(new Callback<ReplyAnalysisResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReplyAnalysisResponse> call, @NonNull Response<ReplyAnalysisResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("Failed to analyze reply");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReplyAnalysisResponse> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}
