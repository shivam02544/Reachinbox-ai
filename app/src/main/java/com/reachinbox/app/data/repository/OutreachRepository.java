package com.reachinbox.app.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.reachinbox.app.data.api.ApiClient;
import com.reachinbox.app.data.api.OutreachApi;
import com.reachinbox.app.data.model.GenerateOutreachRequest;
import com.reachinbox.app.data.model.OutreachGenerationResult;
import com.reachinbox.app.data.model.OutreachResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutreachRepository {

    private final OutreachApi outreachApi;

    public OutreachRepository(Context context) {
        this.outreachApi = ApiClient.getClient(context).create(OutreachApi.class);
    }

    public interface OutreachCallback {
        void onSuccess(OutreachGenerationResult result);
        void onError(String message);
    }

    public void generateOutreach(
            String jobRequirementId,
            GenerateOutreachRequest.Recipient recipient,
            String instructions,
            String tone,
            OutreachCallback callback
    ) {
        GenerateOutreachRequest request = new GenerateOutreachRequest(jobRequirementId, recipient, instructions, tone);

        outreachApi.generateOutreach(request).enqueue(new Callback<OutreachResponse>() {
            @Override
            public void onResponse(@NonNull Call<OutreachResponse> call, @NonNull Response<OutreachResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        callback.onSuccess(response.body().getData());
                    } else {
                        callback.onError(response.body().getMessage() != null ? response.body().getMessage() : "Failed to generate outreach");
                    }
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<OutreachResponse> call, @NonNull Throwable t) {
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
