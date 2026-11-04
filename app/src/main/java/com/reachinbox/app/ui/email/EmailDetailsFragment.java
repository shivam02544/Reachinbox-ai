package com.reachinbox.app.ui.email;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.reachinbox.app.R;
import com.reachinbox.app.data.model.Email;
import com.reachinbox.app.data.model.InboundEmail;
import com.reachinbox.app.data.model.ReplyAnalysis;
import com.reachinbox.app.data.model.ScheduledEmail;
import com.reachinbox.app.data.model.ThreadResponse;
import com.reachinbox.app.data.repository.EmailRepository;
import com.reachinbox.app.data.repository.ReplyRepository;
import com.reachinbox.app.databinding.FragmentEmailDetailsBinding;
import com.reachinbox.app.databinding.ItemInboundEmailBinding;
import com.reachinbox.app.utils.FeedbackUtils;

import java.util.List;

public class EmailDetailsFragment extends Fragment {

    private FragmentEmailDetailsBinding binding;
    private EmailRepository emailRepository;
    private ReplyRepository replyRepository;
    private String emailId;
    private Email currentEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEmailDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emailRepository = new EmailRepository(requireContext());
        replyRepository = new ReplyRepository(requireContext());

        if (getArguments() != null) {
            emailId = getArguments().getString("emailId");
        }

        if (emailId != null) {
            loadEmailDetails();
            loadThread();
        } else {
            FeedbackUtils.showError(binding.getRoot(), getString(R.string.error_load_email));
            requireActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void loadEmailDetails() {
        setLoading(true);
        emailRepository.getEmailDetails(emailId, new EmailRepository.EmailCallback<Email>() {
            @Override
            public void onSuccess(Email result) {
                if (isAdded()) {
                    setLoading(false);
                    currentEmail = result;
                    updateUI(result);
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
    }

    private void loadThread() {
        emailRepository.getEmailThread(emailId, new EmailRepository.EmailCallback<ThreadResponse.ThreadData>() {
            @Override
            public void onSuccess(ThreadResponse.ThreadData result) {
                if (isAdded() && result.getInboundEmails() != null && !result.getInboundEmails().isEmpty()) {
                    displayInboundReplies(result.getInboundEmails());
                }
            }

            @Override
            public void onError(String message) {
                // Silently ignore or log thread errors
            }
        });
    }

    private void displayInboundReplies(List<InboundEmail> inboundEmails) {
        binding.layoutInboundContainer.setVisibility(View.VISIBLE);
        binding.layoutRepliesList.removeAllViews();

        for (InboundEmail inbound : inboundEmails) {
            ItemInboundEmailBinding itemBinding = ItemInboundEmailBinding.inflate(getLayoutInflater(), binding.layoutRepliesList, false);
            
            itemBinding.tvInboundSender.setText(inbound.getFromEmail());
            itemBinding.tvInboundBody.setText(inbound.getTextBody() != null ? inbound.getTextBody() : inbound.getHtmlBody());

            if (inbound.getAnalysis() != null) {
                renderAnalysis(itemBinding, inbound.getAnalysis());
            } else {
                itemBinding.btnAnalyzeReply.setVisibility(View.VISIBLE);
                itemBinding.btnAnalyzeReply.setOnClickListener(v -> analyzeReply(inbound.getId(), itemBinding));
            }

            binding.layoutRepliesList.addView(itemBinding.getRoot());
        }
    }

    private void analyzeReply(String inboundId, ItemInboundEmailBinding itemBinding) {
        itemBinding.btnAnalyzeReply.setEnabled(false);
        itemBinding.btnAnalyzeReply.setText("Analyzing...");

        replyRepository.analyzeReply(inboundId, new ReplyRepository.ReplyCallback() {
            @Override
            public void onSuccess(ReplyAnalysis result) {
                if (isAdded()) {
                    renderAnalysis(itemBinding, result);
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    itemBinding.btnAnalyzeReply.setEnabled(true);
                    itemBinding.btnAnalyzeReply.setText("Analyze Reply with AI");
                    FeedbackUtils.showError(binding.getRoot(), message);
                }
            }
        });
    }

    private void renderAnalysis(ItemInboundEmailBinding itemBinding, ReplyAnalysis analysis) {
        itemBinding.btnAnalyzeReply.setVisibility(View.GONE);
        itemBinding.tvInboundClassification.setVisibility(View.VISIBLE);
        itemBinding.tvInboundClassification.setText(analysis.getClassification());

        itemBinding.layoutAnalysis.setVisibility(View.VISIBLE);
        itemBinding.tvSummary.setText(analysis.getSummary());

        if (analysis.getSuggestedResponse() != null) {
            itemBinding.btnUseSuggestedReply.setVisibility(View.VISIBLE);
            itemBinding.btnUseSuggestedReply.setOnClickListener(v -> {
                if (currentEmail != null && currentEmail.getSenderAccount() != null) {
                    com.reachinbox.app.data.model.CreateDraftRequest draftReq = new com.reachinbox.app.data.model.CreateDraftRequest(
                            currentEmail.getSenderAccount().getId(),
                            currentEmail.getRecipient(),
                            analysis.getSuggestedResponse().getSubject(),
                            analysis.getSuggestedResponse().getBodyText(),
                            false
                    );
                    draftReq.setParentEmailId(currentEmail.getId());

                    emailRepository.saveDraft(draftReq, new EmailRepository.EmailCallback<Email>() {
                        @Override
                        public void onSuccess(Email draft) {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), "Draft created from suggestion", Toast.LENGTH_SHORT).show();
                                Bundle args = new Bundle();
                                args.putString("emailId", draft.getId());
                                Navigation.findNavController(v).navigate(R.id.composeEmailFragment, args);
                            }
                        }

                        @Override
                        public void onError(String message) {
                            if (isAdded()) {
                                FeedbackUtils.showError(binding.getRoot(), "Failed to save draft: " + message);
                            }
                        }
                    });
                }
            });
        } else {
            itemBinding.btnUseSuggestedReply.setVisibility(View.GONE);
        }
    }

    private void updateUI(Email email) {
        binding.tvSubject.setText(email.getSubject());
        
        if (email.getSenderAccount() != null) {
            binding.tvFrom.setText(email.getSenderAccount().getEmail());
        } else {
            binding.tvFrom.setText("Unknown");
        }
        
        binding.tvTo.setText(email.getRecipient());
        binding.tvStatus.setText(email.getStatus());
        
        // Status styling
        int backgroundRes = R.drawable.bg_badge_neutral;
        int textColorRes = R.color.colorOnNeutralContainer;
        
        if (email.getStatus() != null) {
            switch (email.getStatus()) {
                case "SENT":
                    backgroundRes = R.drawable.bg_badge_success;
                    textColorRes = R.color.colorOnSuccessContainer;
                    break;
                case "FAILED":
                    backgroundRes = R.drawable.bg_badge_error;
                    textColorRes = R.color.colorOnErrorContainer;
                    break;
                case "PROCESSING":
                case "SCHEDULED":
                    backgroundRes = R.drawable.bg_badge_info;
                    textColorRes = R.color.colorOnInfoContainer;
                    break;
            }
        }
        binding.tvStatus.setBackgroundResource(backgroundRes);
        binding.tvStatus.setTextColor(requireContext().getColor(textColorRes));

        // Use Text body
        binding.tvBody.setText(email.getTextBody() != null ? email.getTextBody() : "No content");

        // Format dates
        String dateFormat = "MMM dd, yyyy, HH:mm";

        if (email.getScheduledEmails() != null && !email.getScheduledEmails().isEmpty()) {
            ScheduledEmail scheduled = email.getScheduledEmails().get(0);
            binding.layoutScheduled.setVisibility(View.VISIBLE);
            binding.tvScheduledAt.setText(DateFormat.format(dateFormat, scheduled.getScheduledAt()));
        }

        binding.tvDate.setText(DateFormat.format(dateFormat, email.getCreatedAt()));

        // Show AI Follow-Up button if email is SENT
        if ("SENT".equals(email.getStatus())) {
            binding.btnFollowUp.setVisibility(View.VISIBLE);
            binding.btnFollowUp.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("parentEmailId", email.getId());
                Navigation.findNavController(v).navigate(R.id.action_emailDetailsFragment_to_followUpFragment, args);
            });
        } else {
            binding.btnFollowUp.setVisibility(View.GONE);
        }

        if ("FAILED".equals(email.getStatus())) {
            binding.layoutFailedActions.setVisibility(View.VISIBLE);
            binding.btnResend.setOnClickListener(v -> handleResendFailedEmail());
            binding.btnReschedule.setOnClickListener(v -> handleResendFailedEmail());
        } else {
            binding.layoutFailedActions.setVisibility(View.GONE);
        }

        setLoading(false);
    }

    private void handleResendFailedEmail() {
        if (currentEmail == null || currentEmail.getSenderAccount() == null) return;
        
        com.reachinbox.app.data.model.CreateDraftRequest draftReq = new com.reachinbox.app.data.model.CreateDraftRequest(
                currentEmail.getSenderAccount().getId(),
                currentEmail.getRecipient(),
                currentEmail.getSubject(),
                currentEmail.getTextBody(),
                false
        );
        if (currentEmail.getParentEmailId() != null) {
            draftReq.setParentEmailId(currentEmail.getParentEmailId());
            if (currentEmail.getFollowUpNumber() != null) {
                draftReq.setFollowUpNumber(currentEmail.getFollowUpNumber());
            }
        }

        setLoading(true);
        emailRepository.saveDraft(draftReq, new EmailRepository.EmailCallback<Email>() {
            @Override
            public void onSuccess(Email draft) {
                if (isAdded()) {
                    setLoading(false);
                    Toast.makeText(requireContext(), "Opening failed email for retry...", Toast.LENGTH_SHORT).show();
                    Bundle args = new Bundle();
                    args.putString("emailId", draft.getId());
                    Navigation.findNavController(requireView()).navigate(R.id.composeEmailFragment, args);
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    setLoading(false);
                    FeedbackUtils.showError(binding.getRoot(), "Failed to prepare retry: " + message);
                }
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
