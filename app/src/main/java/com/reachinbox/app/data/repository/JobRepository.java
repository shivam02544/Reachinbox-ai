package com.reachinbox.app.data.repository;

import android.content.Context;
import androidx.annotation.NonNull;

import com.reachinbox.app.data.api.ApiClient;
import com.reachinbox.app.data.api.JobApi;
import com.reachinbox.app.data.model.CreateJobRequest;
import com.reachinbox.app.data.model.GenerateOutreachRequest;
import com.reachinbox.app.data.model.JobFromUrlRequest;
import com.reachinbox.app.data.model.JobFromUrlResponse;
import com.reachinbox.app.data.model.JobFromUrlResult;
import com.reachinbox.app.data.model.JobRequirement;
import com.reachinbox.app.data.model.JobRequirementResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JobRepository {
    private final JobApi jobApi;

    public JobRepository(Context context) {
        this.jobApi = ApiClient.getClient(context).create(JobApi.class);
    }

    public interface JobCallback {
        void onSuccess(JobRequirement jobRequirement);
        void onError(String message);
    }

    public interface JobFromUrlCallback {
        void onSuccess(JobFromUrlResult result);
        void onError(String message);
    }

    public void createJobRequirement(String title, String company, String description, JobCallback callback) {
        CreateJobRequest request = new CreateJobRequest(title, company, description);
        jobApi.createJobRequirement(request).enqueue(new Callback<JobRequirementResponse>() {
            @Override
            public void onResponse(@NonNull Call<JobRequirementResponse> call, @NonNull Response<JobRequirementResponse> response) {
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
            public void onFailure(@NonNull Call<JobRequirementResponse> call, @NonNull Throwable t) {
                callback.onError(getFailureMessage(t));
            }
        });
    }

    public void createFromUrl(String url, GenerateOutreachRequest.Recipient recipient, String tone, JobFromUrlCallback callback) {
        JobFromUrlRequest request = recipient != null
                ? new JobFromUrlRequest(url, recipient, tone)
                : new JobFromUrlRequest(url);
        jobApi.createFromUrl(request).enqueue(new Callback<JobFromUrlResponse>() {
            @Override
            public void onResponse(@NonNull Call<JobFromUrlResponse> call, @NonNull Response<JobFromUrlResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess() && response.body().getData() != null) {
                        callback.onSuccess(response.body().getData());
                    } else {
                        callback.onError(response.body().getMessage() != null ? response.body().getMessage() : "Failed to import job URL");
                    }
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<JobFromUrlResponse> call, @NonNull Throwable t) {
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
