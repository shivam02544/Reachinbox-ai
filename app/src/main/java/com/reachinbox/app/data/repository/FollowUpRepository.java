package com.reachinbox.app.data.repository;

import android.content.Context;
import androidx.annotation.NonNull;

import com.reachinbox.app.data.api.ApiClient;
import com.reachinbox.app.data.api.FollowUpApi;
import com.reachinbox.app.data.model.FollowUpGenerationResult;
import com.reachinbox.app.data.model.FollowUpResponse;
import com.reachinbox.app.data.model.GenerateFollowUpRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FollowUpRepository {

    private final FollowUpApi followUpApi;

    public FollowUpRepository(Context context) {
        this.followUpApi = ApiClient.getClient(context).create(FollowUpApi.class);
    }

    public interface FollowUpCallback {
        void onSuccess(FollowUpGenerationResult result);
        void onError(String message);
    }

    public void generateFollowUp(
            String parentEmailId,
            String instructions,
            int followUpNumber,
            FollowUpCallback callback
    ) {
        GenerateFollowUpRequest request = new GenerateFollowUpRequest(parentEmailId, instructions, followUpNumber);

        followUpApi.generateFollowUp(request).enqueue(new Callback<FollowUpResponse>() {
            @Override
            public void onResponse(@NonNull Call<FollowUpResponse> call, @NonNull Response<FollowUpResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        callback.onSuccess(response.body().getData());
                    } else {
                        callback.onError(response.body().getMessage() != null ? response.body().getMessage() : "Failed to generate follow-up");
                    }
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<FollowUpResponse> call, @NonNull Throwable t) {
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
        } catch (Exception ignored) {
        }
        return "Server error: " + response.code();
    }

    private String getFailureMessage(Throwable t) {
        if (t instanceof java.net.SocketTimeoutException) {
            return "Generation timed out. Please try again.";
        } else if (t instanceof java.io.IOException) {
            return "Network error. Please check your connection.";
        }
        return "Unknown error: " + t.getMessage();
    }
}
