package com.reachinbox.app.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.reachinbox.app.data.api.ApiClient;
import com.reachinbox.app.data.api.CampaignApi;
import com.reachinbox.app.data.model.AddRecipientsRequest;
import com.reachinbox.app.data.model.AuthResponse;
import com.reachinbox.app.data.model.Campaign;
import com.reachinbox.app.data.model.CampaignListResponse;
import com.reachinbox.app.data.model.CreateCampaignRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CampaignRepository {
    private final CampaignApi campaignApi;

    public CampaignRepository(Context context) {
        this.campaignApi = ApiClient.getClient(context).create(CampaignApi.class);
    }

    public interface CampaignCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    public void createCampaign(CreateCampaignRequest request, CampaignCallback<Campaign> callback) {
        campaignApi.createCampaign(request).enqueue(new Callback<AuthResponse<Campaign>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Campaign>> call, @NonNull Response<AuthResponse<Campaign>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Campaign>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getCampaigns(int page, int limit, CampaignCallback<CampaignListResponse> callback) {
        campaignApi.getCampaigns(page, limit).enqueue(new Callback<AuthResponse<CampaignListResponse>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<CampaignListResponse>> call, @NonNull Response<AuthResponse<CampaignListResponse>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<CampaignListResponse>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getCampaignDetails(String id, CampaignCallback<Campaign> callback) {
        getCampaignDetails(id, 1, 50, null, callback);
    }

    public void getCampaignDetails(String id, int recipientPage, int recipientLimit, String recipientSearch,
                                   CampaignCallback<Campaign> callback) {
        campaignApi.getCampaignDetails(id, recipientPage, recipientLimit, recipientSearch).enqueue(new Callback<AuthResponse<Campaign>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Campaign>> call, @NonNull Response<AuthResponse<Campaign>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Campaign>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void duplicateCampaign(CreateCampaignRequest request,
                                  AddRecipientsRequest recipients, CampaignCallback<Campaign> callback) {
        createCampaign(request, new CampaignCallback<Campaign>() {
            @Override
            public void onSuccess(Campaign duplicate) {
                addRecipients(duplicate.getId(), recipients, new CampaignCallback<Object>() {
                    @Override
                    public void onSuccess(Object ignored) {
                        markReady(duplicate.getId(), callback);
                    }

                    @Override
                    public void onError(String message) {
                        deleteCampaign(duplicate.getId(), new CampaignCallback<Void>() {
                            @Override public void onSuccess(Void ignored) {
                                callback.onError("Could not copy recipients: " + message);
                            }
                            @Override public void onError(String cleanupError) {
                                callback.onError("Copy incomplete and cleanup failed. " + message);
                            }
                        });
                    }
                });
            }

            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    public void updateCampaign(String id, CreateCampaignRequest request, CampaignCallback<Campaign> callback) {
        campaignApi.updateCampaign(id, request).enqueue(new Callback<AuthResponse<Campaign>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Campaign>> call, @NonNull Response<AuthResponse<Campaign>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Campaign>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void deleteCampaign(String id, CampaignCallback<Void> callback) {
        campaignApi.deleteCampaign(id).enqueue(new Callback<AuthResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Void>> call, @NonNull Response<AuthResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError("Failed to delete campaign");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Void>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void addRecipients(String id, AddRecipientsRequest request, CampaignCallback<Object> callback) {
        campaignApi.addRecipients(id, request).enqueue(new Callback<AuthResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Object>> call, @NonNull Response<AuthResponse<Object>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Object>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void markReady(String id, CampaignCallback<Campaign> callback) {
        campaignApi.markReady(id).enqueue(new Callback<AuthResponse<Campaign>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Campaign>> call, @NonNull Response<AuthResponse<Campaign>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Campaign>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void startCampaign(String id, CampaignCallback<Campaign> callback) {
        campaignApi.startCampaign(id).enqueue(new Callback<AuthResponse<Campaign>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Campaign>> call, @NonNull Response<AuthResponse<Campaign>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Campaign>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void pauseCampaign(String id, CampaignCallback<Campaign> callback) {
        campaignApi.pauseCampaign(id).enqueue(new Callback<AuthResponse<Campaign>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Campaign>> call, @NonNull Response<AuthResponse<Campaign>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Campaign>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void resumeCampaign(String id, CampaignCallback<Campaign> callback) {
        campaignApi.resumeCampaign(id).enqueue(new Callback<AuthResponse<Campaign>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Campaign>> call, @NonNull Response<AuthResponse<Campaign>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Campaign>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void cancelCampaign(String id, CampaignCallback<Campaign> callback) {
        campaignApi.cancelCampaign(id).enqueue(new Callback<AuthResponse<Campaign>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Campaign>> call, @NonNull Response<AuthResponse<Campaign>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Campaign>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void retryFailedRecipients(String id, CampaignCallback<Object> callback) {
        campaignApi.retryFailedRecipients(id).enqueue(new Callback<AuthResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Object>> call, @NonNull Response<AuthResponse<Object>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Object>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    private <T> void handleResponse(Response<AuthResponse<T>> response, CampaignCallback<T> callback) {
        if (response.isSuccessful() && response.body() != null) {
            if (response.body().isSuccess()) {
                callback.onSuccess(response.body().getData());
            } else {
                callback.onError(response.body().getMessage());
            }
        } else {
            callback.onError("API Error: " + response.code());
        }
    }
}
