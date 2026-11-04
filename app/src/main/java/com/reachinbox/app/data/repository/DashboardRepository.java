package com.reachinbox.app.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.reachinbox.app.data.api.ApiClient;
import com.reachinbox.app.data.api.DashboardApi;
import com.reachinbox.app.data.model.AuthResponse;
import com.reachinbox.app.data.model.DashboardData;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardRepository {
    private final DashboardApi dashboardApi;

    public DashboardRepository(Context context) {
        this.dashboardApi = ApiClient.getClient(context).create(DashboardApi.class);
    }

    public interface DashboardCallback {
        void onSuccess(DashboardData data);
        void onError(String message);
    }

    public void getDashboard(DashboardCallback callback) {
        dashboardApi.getDashboard().enqueue(new Callback<AuthResponse<DashboardData>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<DashboardData>> call, @NonNull Response<AuthResponse<DashboardData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        callback.onSuccess(response.body().getData());
                    } else {
                        callback.onError(response.body().getMessage());
                    }
                } else {
                    callback.onError("Failed to load dashboard: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<DashboardData>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}
