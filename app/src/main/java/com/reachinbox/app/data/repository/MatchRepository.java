package com.reachinbox.app.data.repository;

import android.content.Context;
import androidx.annotation.NonNull;

import com.reachinbox.app.data.api.ApiClient;
import com.reachinbox.app.data.api.MatchApi;
import com.reachinbox.app.data.model.GenerateMatchRequest;
import com.reachinbox.app.data.model.MatchAnalysis;
import com.reachinbox.app.data.model.MatchResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchRepository {
    private final MatchApi matchApi;

    public MatchRepository(Context context) {
        this.matchApi = ApiClient.getClient(context).create(MatchApi.class);
    }

    public interface MatchCallback {
        void onSuccess(MatchAnalysis matchAnalysis);
        void onError(String message);
    }

    public void generateMatch(String jobRequirementId, MatchCallback callback) {
        GenerateMatchRequest request = new GenerateMatchRequest(jobRequirementId);
        matchApi.generateMatch(request).enqueue(new Callback<MatchResponse>() {
            @Override
            public void onResponse(@NonNull Call<MatchResponse> call, @NonNull Response<MatchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        callback.onSuccess(response.body().getData());
                    } else {
                        callback.onError(response.body().getMessage());
                    }
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MatchResponse> call, @NonNull Throwable t) {
                callback.onError(getFailureMessage(t));
            }
        });
    }

    private String getErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorStr = response.errorBody().string();
                com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(errorStr).getAsJsonObject();
                if (jsonObject.has("message") && !jsonObject.get("message").isJsonNull()) {
                    return jsonObject.get("message").getAsString();
                }
            }
        } catch (Exception ignored) { }
        return "Server error: " + response.code();
    }

    private String getFailureMessage(Throwable t) {
        if (t instanceof java.net.SocketTimeoutException) {
            return "Server connection timed out.";
        } else if (t instanceof java.io.IOException) {
            return "Network error. Please check your connection.";
        }
        return "Unknown error: " + t.getMessage();
    }
}
