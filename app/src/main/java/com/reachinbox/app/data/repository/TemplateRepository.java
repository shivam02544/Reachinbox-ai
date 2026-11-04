package com.reachinbox.app.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.reachinbox.app.data.api.ApiClient;
import com.reachinbox.app.data.api.TemplateApi;
import com.reachinbox.app.data.model.AuthResponse;
import com.reachinbox.app.data.model.CreateTemplateRequest;
import com.reachinbox.app.data.model.Template;
import com.reachinbox.app.data.model.TemplateListResponse;
import com.reachinbox.app.data.model.UpdateTemplateRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TemplateRepository {
    private final TemplateApi templateApi;

    public TemplateRepository(Context context) {
        this.templateApi = ApiClient.getClient(context).create(TemplateApi.class);
    }

    public interface TemplateCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    public void createTemplate(CreateTemplateRequest request, TemplateCallback<Template> callback) {
        templateApi.createTemplate(request).enqueue(new Callback<AuthResponse<Template>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Template>> call, @NonNull Response<AuthResponse<Template>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Template>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getTemplates(int page, int limit, String search, String sortBy, String sortOrder, TemplateCallback<TemplateListResponse> callback) {
        templateApi.getTemplates(page, limit, search, sortBy, sortOrder).enqueue(new Callback<AuthResponse<TemplateListResponse>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<TemplateListResponse>> call, @NonNull Response<AuthResponse<TemplateListResponse>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<TemplateListResponse>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getTemplate(String id, TemplateCallback<Template> callback) {
        templateApi.getTemplate(id).enqueue(new Callback<AuthResponse<Template>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Template>> call, @NonNull Response<AuthResponse<Template>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Template>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void updateTemplate(String id, UpdateTemplateRequest request, TemplateCallback<Template> callback) {
        templateApi.updateTemplate(id, request).enqueue(new Callback<AuthResponse<Template>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Template>> call, @NonNull Response<AuthResponse<Template>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Template>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void deleteTemplate(String id, TemplateCallback<Void> callback) {
        templateApi.deleteTemplate(id).enqueue(new Callback<AuthResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Void>> call, @NonNull Response<AuthResponse<Void>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError("Failed to delete template");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Void>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    private <T> void handleResponse(Response<AuthResponse<T>> response, TemplateCallback<T> callback) {
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
