package com.reachinbox.app.data.api;

import com.reachinbox.app.data.model.AuthResponse;
import com.reachinbox.app.data.model.CreateDraftRequest;
import com.reachinbox.app.data.model.CreateEmailRequest;
import com.reachinbox.app.data.model.Email;
import com.reachinbox.app.data.model.EmailListResponse;
import com.reachinbox.app.data.model.UpdateDraftRequest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface EmailApi {
    @POST("emails")
    Call<AuthResponse<EmailResponseData>> sendEmail(@Body CreateEmailRequest request);

    @GET("emails")
    Call<AuthResponse<EmailListResponse>> getEmails(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("search") String search,
            @Query("status") String status,
            @Query("senderAccountId") String senderAccountId,
            @Query("from") String from,
            @Query("to") String to,
            @Query("sortBy") String sortBy,
            @Query("sortOrder") String sortOrder
    );

    @GET("emails/{id}")
    Call<AuthResponse<EmailResponseData>> getEmailDetails(@Path("id") String id);

    @GET("emails/{id}/thread")
    Call<com.reachinbox.app.data.model.ThreadResponse> getEmailThread(@Path("id") String id);

    /**
     * Draft Endpoints
     */
    @POST("emails/drafts")
    Call<AuthResponse<Map<String, Email>>> saveDraft(@Body CreateDraftRequest request);

    @GET("emails/drafts")
    Call<AuthResponse<EmailListResponse>> getDrafts(@Query("page") int page, @Query("limit") int limit);

    @PATCH("emails/drafts/{id}")
    Call<AuthResponse<Map<String, Email>>> updateDraft(@Path("id") String id, @Body UpdateDraftRequest request);

    @DELETE("emails/drafts/{id}")
    Call<AuthResponse<Void>> deleteDraft(@Path("id") String id);

    @POST("emails/drafts/{id}/send")
    Call<AuthResponse<EmailResponseData>> sendDraft(@Path("id") String id, @Body Map<String, String> body);

    @POST("emails/drafts/{id}/schedule")
    Call<AuthResponse<Map<String, Object>>> scheduleDraft(@Path("id") String id, @Body Map<String, String> body);

    class EmailResponseData {
        private Email email;
        public Email getEmail() { return email; }
    }
}
