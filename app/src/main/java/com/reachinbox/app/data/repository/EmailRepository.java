package com.reachinbox.app.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.reachinbox.app.data.api.ApiClient;
import com.reachinbox.app.data.api.EmailApi;
import com.reachinbox.app.data.model.AuthResponse;
import com.reachinbox.app.data.model.CreateDraftRequest;
import com.reachinbox.app.data.model.CreateEmailRequest;
import com.reachinbox.app.data.model.Email;
import com.reachinbox.app.data.model.EmailListResponse;
import com.reachinbox.app.data.model.UpdateDraftRequest;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmailRepository {
    private final EmailApi emailApi;

    public EmailRepository(Context context) {
        this.emailApi = ApiClient.getClient(context).create(EmailApi.class);
    }

    public interface EmailCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    public void sendEmail(CreateEmailRequest request, EmailCallback<Email> callback) {
        emailApi.sendEmail(request).enqueue(new Callback<AuthResponse<EmailApi.EmailResponseData>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<EmailApi.EmailResponseData>> call, @NonNull Response<AuthResponse<EmailApi.EmailResponseData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        callback.onSuccess(response.body().getData().getEmail());
                    } else {
                        callback.onError(response.body().getMessage());
                    }
                } else {
                    callback.onError("Failed to send email: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<EmailApi.EmailResponseData>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getEmails(int page, int limit, String search, String status, String senderAccountId, 
                         String from, String to, String sortBy, String sortOrder, 
                         EmailCallback<EmailListResponse> callback) {
        emailApi.getEmails(page, limit, search, status, senderAccountId, from, to, sortBy, sortOrder).enqueue(new Callback<AuthResponse<EmailListResponse>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<EmailListResponse>> call, @NonNull Response<AuthResponse<EmailListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        callback.onSuccess(response.body().getData());
                    } else {
                        callback.onError(response.body().getMessage());
                    }
                } else {
                    callback.onError("Failed to load emails: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<EmailListResponse>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void saveDraft(CreateDraftRequest request, EmailCallback<Email> callback) {
        emailApi.saveDraft(request).enqueue(new Callback<AuthResponse<Map<String, Email>>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Map<String, Email>>> call, @NonNull Response<AuthResponse<Map<String, Email>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getData().get("draft"));
                } else {
                    callback.onError("Failed to save draft");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Map<String, Email>>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getDrafts(int page, int limit, EmailCallback<EmailListResponse> callback) {
        emailApi.getDrafts(page, limit).enqueue(new Callback<AuthResponse<EmailListResponse>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<EmailListResponse>> call, @NonNull Response<AuthResponse<EmailListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("Failed to load drafts");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<EmailListResponse>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void updateDraft(String id, UpdateDraftRequest request, EmailCallback<Email> callback) {
        emailApi.updateDraft(id, request).enqueue(new Callback<AuthResponse<Map<String, Email>>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Map<String, Email>>> call, @NonNull Response<AuthResponse<Map<String, Email>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getData().get("draft"));
                } else {
                    callback.onError("Failed to update draft");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Map<String, Email>>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void deleteDraft(String id, EmailCallback<Void> callback) {
        emailApi.deleteDraft(id).enqueue(new Callback<AuthResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Void>> call, @NonNull Response<AuthResponse<Void>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError("Failed to delete draft");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Void>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void sendDraft(String id, String idempotencyKey, EmailCallback<Email> callback) {
        Map<String, String> body = new HashMap<>();
        body.put("idempotencyKey", idempotencyKey);
        emailApi.sendDraft(id, body).enqueue(new Callback<AuthResponse<EmailApi.EmailResponseData>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<EmailApi.EmailResponseData>> call, @NonNull Response<AuthResponse<EmailApi.EmailResponseData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getData().getEmail());
                } else {
                    callback.onError("Failed to send draft");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<EmailApi.EmailResponseData>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void scheduleDraft(String id, String scheduledAt, EmailCallback<Object> callback) {
        Map<String, String> body = new HashMap<>();
        body.put("scheduledAt", scheduledAt);
        emailApi.scheduleDraft(id, body).enqueue(new Callback<AuthResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Map<String, Object>>> call, @NonNull Response<AuthResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getData().get("scheduledEmail"));
                } else {
                    callback.onError("Failed to schedule draft");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Map<String, Object>>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getEmailDetails(String id, EmailCallback<Email> callback) {
        emailApi.getEmailDetails(id).enqueue(new Callback<AuthResponse<EmailApi.EmailResponseData>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<EmailApi.EmailResponseData>> call, @NonNull Response<AuthResponse<EmailApi.EmailResponseData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess() && response.body().getData() != null) {
                        callback.onSuccess(response.body().getData().getEmail());
                    } else {
                        callback.onError(response.body().getMessage() != null ? response.body().getMessage() : "Unknown error");
                    }
                } else {
                    callback.onError("Failed to load email details: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<EmailApi.EmailResponseData>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getEmailThread(String id, EmailCallback<com.reachinbox.app.data.model.ThreadResponse.ThreadData> callback) {
        emailApi.getEmailThread(id).enqueue(new Callback<com.reachinbox.app.data.model.ThreadResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.reachinbox.app.data.model.ThreadResponse> call, @NonNull Response<com.reachinbox.app.data.model.ThreadResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("Failed to load conversation thread");
                }
            }

            @Override
            public void onFailure(@NonNull Call<com.reachinbox.app.data.model.ThreadResponse> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}
