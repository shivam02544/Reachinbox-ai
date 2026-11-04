package com.reachinbox.app.data.api;

import com.reachinbox.app.data.model.AddRecipientsRequest;
import com.reachinbox.app.data.model.AuthResponse;
import com.reachinbox.app.data.model.Campaign;
import com.reachinbox.app.data.model.CampaignListResponse;
import com.reachinbox.app.data.model.CreateCampaignRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CampaignApi {
    @POST("campaigns")
    Call<AuthResponse<Campaign>> createCampaign(@Body CreateCampaignRequest request);

    @GET("campaigns")
    Call<AuthResponse<CampaignListResponse>> getCampaigns(@Query("page") int page, @Query("limit") int limit);

    @GET("campaigns/{id}")
    Call<AuthResponse<Campaign>> getCampaignDetails(@Path("id") String id,
                                                     @Query("recipientPage") int recipientPage,
                                                     @Query("recipientLimit") int recipientLimit,
                                                     @Query("recipientSearch") String recipientSearch);

    @PATCH("campaigns/{id}")
    Call<AuthResponse<Campaign>> updateCampaign(@Path("id") String id, @Body CreateCampaignRequest request);

    @DELETE("campaigns/{id}")
    Call<AuthResponse<Void>> deleteCampaign(@Path("id") String id);

    @POST("campaigns/{id}/recipients")
    Call<AuthResponse<Object>> addRecipients(@Path("id") String id, @Body AddRecipientsRequest request);

    @POST("campaigns/{id}/ready")
    Call<AuthResponse<Campaign>> markReady(@Path("id") String id);

    @POST("campaigns/{id}/start")
    Call<AuthResponse<Campaign>> startCampaign(@Path("id") String id);

    @POST("campaigns/{id}/pause")
    Call<AuthResponse<Campaign>> pauseCampaign(@Path("id") String id);

    @POST("campaigns/{id}/resume")
    Call<AuthResponse<Campaign>> resumeCampaign(@Path("id") String id);

    @POST("campaigns/{id}/cancel")
    Call<AuthResponse<Campaign>> cancelCampaign(@Path("id") String id);

    @POST("campaigns/{id}/retry-failed")
    Call<AuthResponse<Object>> retryFailedRecipients(@Path("id") String id);

    @DELETE("campaigns/{campaignId}/recipients/{recipientId}")
    Call<AuthResponse<Void>> removeRecipient(@Path("campaignId") String campaignId, @Path("recipientId") String recipientId);
}
