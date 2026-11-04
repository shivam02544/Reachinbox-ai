package com.reachinbox.app.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.reachinbox.app.data.api.ApiClient;
import com.reachinbox.app.data.api.ScheduledEmailApi;
import com.reachinbox.app.data.model.AuthResponse;
import com.reachinbox.app.data.model.CreateScheduledEmailRequest;
import com.reachinbox.app.data.model.ScheduledEmail;
import com.reachinbox.app.data.model.ScheduledEmailListResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScheduledEmailRepository {
    private final ScheduledEmailApi api;

    public ScheduledEmailRepository(Context context) {
        this.api = ApiClient.getClient(context).create(ScheduledEmailApi.class);
    }

    public interface ScheduledEmailCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    public void scheduleEmail(CreateScheduledEmailRequest request, ScheduledEmailCallback<ScheduledEmail> callback) {
        api.scheduleEmail(request).enqueue(new Callback<AuthResponse<ScheduledEmail>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<ScheduledEmail>> call, @NonNull Response<AuthResponse<ScheduledEmail>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<ScheduledEmail>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getScheduledEmails(int page, int limit, ScheduledEmailCallback<ScheduledEmailListResponse> callback) {
        api.getScheduledEmails(page, limit).enqueue(new Callback<AuthResponse<ScheduledEmailListResponse>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<ScheduledEmailListResponse>> call, @NonNull Response<AuthResponse<ScheduledEmailListResponse>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<ScheduledEmailListResponse>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void cancelScheduledEmail(String id, ScheduledEmailCallback<Void> callback) {
        api.cancelScheduledEmail(id).enqueue(new Callback<AuthResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Void>> call, @NonNull Response<AuthResponse<Void>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError("Failed to cancel email");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Void>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    private <T> void handleResponse(Response<AuthResponse<T>> response, ScheduledEmailCallback<T> callback) {
        if (response.isSuccessful() && response.body() != null) {
            if (response.body().isSuccess()) {
                callback.onSuccess(response.body().getData());
            } else {
                callback.onError(response.body().getMessage());
            }
        } else {
            callback.onError("Server error: " + response.code());
        }
    }
}
