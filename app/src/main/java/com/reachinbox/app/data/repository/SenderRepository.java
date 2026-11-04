package com.reachinbox.app.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.reachinbox.app.data.api.ApiClient;
import com.reachinbox.app.data.api.SenderApi;
import com.reachinbox.app.data.model.AuthResponse;
import com.reachinbox.app.data.model.CreateSenderRequest;
import com.reachinbox.app.data.model.Sender;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SenderRepository {
    private final SenderApi senderApi;

    public SenderRepository(Context context) {
        this.senderApi = ApiClient.getClient(context).create(SenderApi.class);
    }

    public interface SenderCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    public void getSenders(SenderCallback<List<Sender>> callback) {
        senderApi.getSenders().enqueue(new Callback<AuthResponse<List<Sender>>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<List<Sender>>> call, @NonNull Response<AuthResponse<List<Sender>>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<List<Sender>>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void createSender(CreateSenderRequest request, SenderCallback<Sender> callback) {
        senderApi.createSender(request).enqueue(new Callback<AuthResponse<Sender>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Sender>> call, @NonNull Response<AuthResponse<Sender>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Sender>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void verifySender(String id, SenderCallback<Sender> callback) {
        senderApi.verifySender(id).enqueue(new Callback<AuthResponse<Sender>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Sender>> call, @NonNull Response<AuthResponse<Sender>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Sender>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void deleteSender(String id, SenderCallback<Void> callback) {
        senderApi.deleteSender(id).enqueue(new Callback<AuthResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Void>> call, @NonNull Response<AuthResponse<Void>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError("Failed to delete sender");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Void>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    private <T> void handleResponse(Response<AuthResponse<T>> response, SenderCallback<T> callback) {
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
