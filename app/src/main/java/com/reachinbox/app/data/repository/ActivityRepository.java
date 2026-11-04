package com.reachinbox.app.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.reachinbox.app.data.api.ActivityApi;
import com.reachinbox.app.data.api.ApiClient;
import com.reachinbox.app.data.model.ActivityListResponse;
import com.reachinbox.app.data.model.AuthResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityRepository {
    private final ActivityApi api;

    public ActivityRepository(Context context) {
        this.api = ApiClient.getClient(context).create(ActivityApi.class);
    }

    public interface ActivityCallback {
        void onSuccess(ActivityListResponse data);
        void onError(String message);
    }

    public void getActivities(int page, int limit, ActivityCallback callback) {
        api.getActivities(page, limit).enqueue(new Callback<AuthResponse<ActivityListResponse>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<ActivityListResponse>> call, @NonNull Response<AuthResponse<ActivityListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        callback.onSuccess(response.body().getData());
                    } else {
                        callback.onError(response.body().getMessage());
                    }
                } else {
                    callback.onError("Failed to load activities: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<ActivityListResponse>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}
