package com.reachinbox.app.ui.campaign;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.reachinbox.app.R;
import com.reachinbox.app.adapter.RecipientAdapter;
import com.reachinbox.app.data.model.Campaign;
import com.reachinbox.app.data.model.CampaignStats;
import com.reachinbox.app.data.repository.CampaignRepository;
import com.reachinbox.app.databinding.FragmentCampaignDetailsBinding;
import com.reachinbox.app.utils.DialogHelper;
import com.reachinbox.app.utils.FeedbackUtils;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CampaignDetailsFragment extends Fragment {

    private FragmentCampaignDetailsBinding binding;
    private CampaignRepository campaignRepository;
    private RecipientAdapter adapter;
    private String campaignId;
    private Campaign currentCampaign;
    private List<com.reachinbox.app.data.model.CampaignRecipient> allRecipients = new ArrayList<>();
    private int recipientPage = 1;
    private boolean recipientSearchPending = false;
    private Runnable recipientSearchRunnable;
    private String pendingCsv;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private static final int POLL_INTERVAL = 5000; // 5 seconds

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCampaignDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        campaignRepository = new CampaignRepository(requireContext());
        
        if (getArguments() != null) {
            campaignId = getArguments().getString("campaignId");
        }

        setupToolbar();
        setupRecyclerView();
        setupListeners();
        setupRecipientTools();
        loadCampaignDetails();
    }

    private void setupRecipientTools() {
        binding.etRecipientSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (recipientSearchRunnable != null) pollHandler.removeCallbacks(recipientSearchRunnable);
                recipientSearchRunnable = () -> {
                    if (binding != null) {
                        recipientPage = 1;
                        allRecipients.clear();
                        loadCampaignDetails(false);
                    }
                };
                pollHandler.postDelayed(recipientSearchRunnable, 350);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        binding.btnExportRecipients.setOnClickListener(v -> exportRecipientsCsv());
        binding.btnDuplicateCampaign.setOnClickListener(v -> confirmDuplicateCampaign());
        binding.rvRecipients.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0 || recipientSearchPending || currentCampaign == null) return;
                CampaignStats stats = currentCampaign.getStats();
                if (stats != null && allRecipients.size() >= stats.getTotal()) return;
                androidx.recyclerview.widget.RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
                if (manager instanceof LinearLayoutManager) {
                    LinearLayoutManager layout = (LinearLayoutManager) manager;
                    if (layout.findLastVisibleItemPosition() >= adapter.getItemCount() - 3) {
                        recipientPage++;
                        recipientSearchPending = true;
                        loadCampaignDetails(false);
                    }
                }
            }
        });
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());
    }

    private void setupRecyclerView() {
        adapter = new RecipientAdapter();
        binding.rvRecipients.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvRecipients.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.swipeRefresh.setOnRefreshListener(this::loadCampaignDetails);
        
        binding.btnStart.setOnClickListener(v -> showConfirmation("Start campaign?", 
            "This will begin sending emails to your recipients.", () -> {
                setLoading(true);
                campaignRepository.startCampaign(campaignId, actionCallback);
            }));

        binding.btnPause.setOnClickListener(v -> showConfirmation("Pause campaign?", 
            "Ongoing sends will finish, but no new sends will start.", () -> {
                setLoading(true);
                campaignRepository.pauseCampaign(campaignId, actionCallback);
            }));

        binding.btnResume.setOnClickListener(v -> showConfirmation("Resume campaign?", 
            "Processing will continue for pending recipients.", () -> {
                setLoading(true);
                campaignRepository.resumeCampaign(campaignId, actionCallback);
            }));

        binding.btnCancel.setOnClickListener(v -> DialogHelper.showDestructiveConfirmation(requireContext(),
                "Cancel campaign?",
                "Pending recipients will not be sent. This cannot be undone.",
                "Cancel Campaign",
                new DialogHelper.DialogCallback() {
                    @Override
                    public void onPositive() {
                        setLoading(true);
                        campaignRepository.cancelCampaign(campaignId, actionCallback);
                    }
                }));

        binding.btnRetryFailed.setOnClickListener(v -> {
            setLoading(true);
            campaignRepository.retryFailedRecipients(campaignId, new CampaignRepository.CampaignCallback<Object>() {
                @Override
                public void onSuccess(Object result) {
                    if (isAdded()) {
                        setLoading(false);
                        FeedbackUtils.showSuccess(binding.getRoot(), getString(R.string.msg_failed_recipients_reset));
                        loadCampaignDetails(true);
                    }
                }

                @Override
                public void onError(String message) {
                    if (isAdded()) {
                        setLoading(false);
                        FeedbackUtils.showError(binding.getRoot(), message);
                    }
                }
            });
        });
    }

    private final CampaignRepository.CampaignCallback<Campaign> actionCallback = new CampaignRepository.CampaignCallback<Campaign>() {
        @Override
        public void onSuccess(Campaign result) {
            if (isAdded()) {
                setLoading(false);
                updateUI(result);
                FeedbackUtils.showSuccess(binding.getRoot(), getString(R.string.msg_campaign_updated));
            }
        }

        @Override
        public void onError(String message) {
            if (isAdded()) {
                setLoading(false);
                FeedbackUtils.showError(binding.getRoot(), message);
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        startPolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPolling();
    }

    private void startPolling() {
        stopPolling(); // Ensure no duplicate handlers
        pollHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentCampaign != null && "RUNNING".equals(currentCampaign.getStatus())) {
                    loadCampaignDetails(false);
                    pollHandler.postDelayed(this, POLL_INTERVAL);
                } else if (currentCampaign != null && ("READY".equals(currentCampaign.getStatus()) || "PAUSED".equals(currentCampaign.getStatus()))) {
                    // Continue polling if it can be started/resumed
                    pollHandler.postDelayed(this, POLL_INTERVAL);
                } else {
                    // Terminal state (COMPLETED, CANCELLED, FAILED, DRAFT) - stop polling
                    stopPolling();
                }
            }
        }, POLL_INTERVAL);
    }

    private void stopPolling() {
        pollHandler.removeCallbacksAndMessages(null);
    }

    private void loadCampaignDetails() {
        loadCampaignDetails(true);
    }

    private void loadCampaignDetails(boolean showSwipeRefresh) {
        if (showSwipeRefresh && binding != null) {
            binding.swipeRefresh.setRefreshing(true);
        }
        String recipientSearch = binding.etRecipientSearch.getText() == null ? null
                : binding.etRecipientSearch.getText().toString().trim();
        campaignRepository.getCampaignDetails(campaignId, recipientPage, 50,
                recipientSearch == null || recipientSearch.isEmpty() ? null : recipientSearch,
                new CampaignRepository.CampaignCallback<Campaign>() {
            @Override
            public void onSuccess(Campaign result) {
                if (isAdded() && binding != null) {
                    binding.swipeRefresh.setRefreshing(false);
                    recipientSearchPending = false;
                    updateUI(result);
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded() && binding != null) {
                    binding.swipeRefresh.setRefreshing(false);
                    recipientSearchPending = false;
                    // Don't show toast for background polling errors to avoid annoyance
                    if (showSwipeRefresh) {
                        FeedbackUtils.showError(binding.getRoot(), message);
                    }
                }
            }
        });
    }

    private void updateUI(Campaign campaign) {
        currentCampaign = campaign;
        binding.tvCampaignName.setText(campaign.getName());
        binding.tvStatus.setText(campaign.getStatus());
        binding.tvSubject.setText(getString(R.string.label_subject_format, campaign.getSubject()));
        
        if (campaign.getSenderAccount() != null) {
            binding.tvSender.setText(getString(R.string.label_from_format, campaign.getSenderAccount().getEmail()));
        }

        CampaignStats stats = campaign.getStats();
        if (stats != null) {
            binding.tvTotalCount.setText(getString(R.string.label_total_count, stats.getTotal()));
            binding.tvSentCount.setText(getString(R.string.label_sent_count, stats.getSent()));
            binding.tvFailedCount.setText(getString(R.string.label_failed_count, stats.getFailed()));
            binding.tvPendingCount.setText(getString(R.string.label_pending_count, (stats.getPending() + stats.getQueued() + stats.getProcessing())));

            binding.progressIndicator.setMax(stats.getTotal());
            binding.progressIndicator.setProgress(stats.getSent() + stats.getFailed());
            
            // Show retry button if there are failed recipients and campaign is in eligible state
            boolean hasFailed = stats.getFailed() > 0;
            boolean eligibleState = "READY".equals(campaign.getStatus()) || "PAUSED".equals(campaign.getStatus()) || "FAILED".equals(campaign.getStatus());
            binding.btnRetryFailed.setVisibility(hasFailed && eligibleState ? View.VISIBLE : View.GONE);
        }

        if (campaign.getRecipients() != null) {
            if (recipientPage == 1) allRecipients.clear();
            for (com.reachinbox.app.data.model.CampaignRecipient recipient : campaign.getRecipients()) {
                boolean exists = false;
                for (com.reachinbox.app.data.model.CampaignRecipient current : allRecipients) {
                    if (current.getId().equals(recipient.getId())) { exists = true; break; }
                }
                if (!exists) allRecipients.add(recipient);
            }
            String query = binding.etRecipientSearch.getText() == null ? ""
                    : binding.etRecipientSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
            List<com.reachinbox.app.data.model.CampaignRecipient> visibleRecipients = new ArrayList<>();
            for (com.reachinbox.app.data.model.CampaignRecipient recipient : allRecipients) {
                if (query.isEmpty() || recipient.getRecipient().toLowerCase(Locale.ROOT).contains(query)) {
                    visibleRecipients.add(recipient);
                }
            }
            adapter.setRecipients(visibleRecipients);
            CampaignStats counts = campaign.getStats();
            int total = counts == null ? allRecipients.size() : counts.getTotal();
            binding.tvRecipientCount.setText(query.isEmpty()
                    ? getString(R.string.msg_recipients_count, total)
                    : getString(R.string.msg_recipients_filtered_count, visibleRecipients.size(), total));
        }

        updateActionButtons(campaign.getStatus());

        // Status styling
        int backgroundRes = R.drawable.bg_badge_neutral;
        int textColorRes = R.color.colorOnNeutralContainer;
        
        switch (campaign.getStatus()) {
            case "DRAFT":
                backgroundRes = R.drawable.bg_badge_neutral;
                textColorRes = R.color.colorOnNeutralContainer;
                break;
            case "READY":
                backgroundRes = R.drawable.bg_badge_info;
                textColorRes = R.color.colorOnInfoContainer;
                break;
            case "RUNNING":
                backgroundRes = R.drawable.bg_badge_info;
                textColorRes = R.color.colorOnInfoContainer;
                break;
            case "PAUSED":
                backgroundRes = R.drawable.bg_badge_warning;
                textColorRes = R.color.colorOnWarningContainer;
                break;
            case "COMPLETED":
                backgroundRes = R.drawable.bg_badge_success;
                textColorRes = R.color.colorOnSuccessContainer;
                break;
            case "CANCELLED":
                backgroundRes = R.drawable.bg_badge_neutral;
                textColorRes = R.color.colorOnNeutralContainer;
                break;
            case "FAILED":
                backgroundRes = R.drawable.bg_badge_error;
                textColorRes = R.color.colorOnErrorContainer;
                break;
        }
        binding.tvStatus.setBackgroundResource(backgroundRes);
        binding.tvStatus.setTextColor(requireContext().getColor(textColorRes));
    }

    private void updateActionButtons(String status) {
        binding.btnStart.setVisibility(View.GONE);
        binding.btnPause.setVisibility(View.GONE);
        binding.btnResume.setVisibility(View.GONE);
        binding.btnCancel.setVisibility(View.GONE);

        switch (status) {
            case "READY":
                binding.btnStart.setVisibility(View.VISIBLE);
                break;
            case "RUNNING":
                binding.btnPause.setVisibility(View.VISIBLE);
                binding.btnCancel.setVisibility(View.VISIBLE);
                break;
            case "PAUSED":
                binding.btnResume.setVisibility(View.VISIBLE);
                binding.btnCancel.setVisibility(View.VISIBLE);
                break;
            case "SCHEDULED":
                binding.btnCancel.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void exportRecipientsCsv() {
        if (allRecipients.isEmpty()) {
            FeedbackUtils.showInfo(binding.getRoot(), getString(R.string.msg_no_recipients_to_export));
            return;
        }
        loadAllRecipients(recipients -> writeRecipientsCsv(recipients));
    }

    private void writeRecipientsCsv(List<com.reachinbox.app.data.model.CampaignRecipient> recipients) {
        if (binding == null || recipients.isEmpty()) return;
        StringBuilder csv = new StringBuilder("email,status,attempts,sent_at,error\n");
        for (com.reachinbox.app.data.model.CampaignRecipient recipient : recipients) {
            csv.append(csvCell(recipient.getRecipient())).append(',')
                    .append(csvCell(recipient.getStatus())).append(',')
                    .append(recipient.getAttempts()).append(',')
                    .append(csvCell(recipient.getSentAt() == null ? ""
                            : DateFormat.format("yyyy-MM-dd'T'HH:mm:ssZ", recipient.getSentAt()).toString())).append(',')
                    .append(csvCell(recipient.getErrorMessage())).append('\n');
        }
        pendingCsv = csv.toString();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "reachinbox-recipients.csv");
        exportRecipientsLauncher.launch(intent);
    }

    private final androidx.activity.result.ActivityResultLauncher<Intent> exportRecipientsLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null
                        && result.getData().getData() != null && pendingCsv != null && binding != null) {
                    try (OutputStream output = requireContext().getContentResolver().openOutputStream(result.getData().getData())) {
                        if (output != null) {
                            output.write(pendingCsv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            FeedbackUtils.showSuccess(binding.getRoot(), getString(R.string.msg_recipient_export_ready));
                            Intent share = new Intent(Intent.ACTION_SEND);
                            share.setType("text/csv");
                            share.putExtra(Intent.EXTRA_STREAM, result.getData().getData());
                            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(Intent.createChooser(share, getString(R.string.label_share_csv)));
                        }
                    } catch (Exception error) {
                        FeedbackUtils.showError(binding.getRoot(), error.getMessage());
                    }
                }
                pendingCsv = null;
            });

    private String csvCell(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private void confirmDuplicateCampaign() {
        if (currentCampaign == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_duplicate_campaign)
                .setMessage(R.string.dialog_duplicate_campaign_message)
                .setNegativeButton(R.string.label_cancel, null)
                .setPositiveButton(R.string.btn_duplicate_campaign, (dialog, which) -> duplicateCampaign())
                .show();
    }

    private void duplicateCampaign() {
        if (currentCampaign == null || currentCampaign.getSenderAccountId() == null
                || currentCampaign.getSubject() == null
                || (currentCampaign.getBodyText() == null && currentCampaign.getBodyHtml() == null)
                || allRecipients.isEmpty()) {
            FeedbackUtils.showError(binding.getRoot(), getString(R.string.msg_campaign_copy_failed, "Campaign content or recipients are incomplete."));
            return;
        }
        loadAllRecipients(this::createCampaignCopy);
    }

    private void createCampaignCopy(List<com.reachinbox.app.data.model.CampaignRecipient> recipients) {
        if (binding == null || currentCampaign == null) return;
        setLoading(true);
        String copyName = currentCampaign.getName() + " (copy)";
        String copyBodyText = currentCampaign.getBodyText() == null ? "" : currentCampaign.getBodyText();
        com.reachinbox.app.data.model.CreateCampaignRequest copy = new com.reachinbox.app.data.model.CreateCampaignRequest(
                copyName, currentCampaign.getSenderAccountId(), currentCampaign.getSubject(), copyBodyText);
        copy.setBodyHtml(currentCampaign.getBodyHtml());
        List<String> recipientEmails = new ArrayList<>();
        for (com.reachinbox.app.data.model.CampaignRecipient recipient : recipients) {
            recipientEmails.add(recipient.getRecipient());
        }
        campaignRepository.duplicateCampaign(copy,
                new com.reachinbox.app.data.model.AddRecipientsRequest(recipientEmails),
                new CampaignRepository.CampaignCallback<Campaign>() {
                    @Override public void onSuccess(Campaign duplicate) {
                        if (!isAdded() || binding == null) return;
                        setLoading(false);
                        FeedbackUtils.showSuccess(binding.getRoot(), getString(R.string.msg_campaign_duplicated));
                        Bundle args = new Bundle();
                        args.putString("campaignId", duplicate.getId());
                        Navigation.findNavController(requireView()).navigate(R.id.action_campaignDetailsFragment_self, args);
                    }
                    @Override public void onError(String message) {
                        if (!isAdded() || binding == null) return;
                        setLoading(false);
                        FeedbackUtils.showError(binding.getRoot(), getString(R.string.msg_campaign_copy_failed, message));
                    }
                });
    }

    private interface RecipientsCallback {
        void onLoaded(List<com.reachinbox.app.data.model.CampaignRecipient> recipients);
    }

    private void loadAllRecipients(RecipientsCallback callback) {
        if (currentCampaign == null || currentCampaign.getStats() == null) {
            callback.onLoaded(new ArrayList<>(allRecipients));
            return;
        }
        List<com.reachinbox.app.data.model.CampaignRecipient> complete = new ArrayList<>();
        fetchRecipientPage(1, complete, callback, currentCampaign.getStats().getTotal());
    }

    private void fetchRecipientPage(int page,
                                    List<com.reachinbox.app.data.model.CampaignRecipient> accumulator,
                                    RecipientsCallback callback, int expectedTotal) {
        campaignRepository.getCampaignDetails(campaignId, page, 100, null,
                new CampaignRepository.CampaignCallback<Campaign>() {
                    @Override public void onSuccess(Campaign result) {
                        if (!isAdded() || binding == null) return;
                        List<com.reachinbox.app.data.model.CampaignRecipient> pageRecipients = result.getRecipients();
                        if (pageRecipients != null) accumulator.addAll(pageRecipients);
                        if (accumulator.size() >= expectedTotal || pageRecipients == null || pageRecipients.isEmpty()) {
                            callback.onLoaded(accumulator);
                        } else {
                            fetchRecipientPage(page + 1, accumulator, callback, expectedTotal);
                        }
                    }
                    @Override public void onError(String message) {
                        if (isAdded() && binding != null) {
                            FeedbackUtils.showError(binding.getRoot(), message);
                            callback.onLoaded(new ArrayList<>(allRecipients));
                        }
                    }
                });
    }

    private void showConfirmation(String title, String message, Runnable onConfirm) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Confirm", (dialog, which) -> onConfirm.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPolling();
        binding = null;
    }
}
