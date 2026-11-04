package com.reachinbox.app.ui.email;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.reachinbox.app.R;
import com.reachinbox.app.data.model.CreateDraftRequest;
import com.reachinbox.app.data.model.CreateEmailRequest;
import com.reachinbox.app.data.model.Email;
import com.reachinbox.app.data.model.Sender;
import com.reachinbox.app.data.model.Template;
import com.reachinbox.app.data.model.TemplateListResponse;
import com.reachinbox.app.data.model.UpdateDraftRequest;
import com.reachinbox.app.data.repository.EmailRepository;
import com.reachinbox.app.data.repository.AiComposeRepository;
import com.reachinbox.app.data.repository.SenderRepository;
import com.reachinbox.app.data.repository.TemplateRepository;
import com.reachinbox.app.databinding.FragmentComposeEmailBinding;
import com.reachinbox.app.utils.FeedbackUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class ComposeEmailFragment extends Fragment {

    private FragmentComposeEmailBinding binding;
    private EmailRepository emailRepository;
    private SenderRepository senderRepository;
    private TemplateRepository templateRepository;
    private AiComposeRepository aiComposeRepository;
    private List<Sender> senders = new ArrayList<>();
    private Sender selectedSender;
    private String emailId; // Non-null if editing a draft
    private String currentIdempotencyKey;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentComposeEmailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emailRepository = new EmailRepository(requireContext());
        senderRepository = new SenderRepository(requireContext());
        templateRepository = new TemplateRepository(requireContext());
        aiComposeRepository = new AiComposeRepository(requireContext());

        if (getArguments() != null) {
            emailId = getArguments().getString("emailId");
        }

        setupButtons();
        
        if (emailId != null) {
            loadDraft();
        } else {
            setupSenderDropdown();
        }
    }

    private void setupButtons() {
        binding.btnSend.setOnClickListener(v -> handleSend());
        binding.btnSaveDraft.setOnClickListener(v -> handleSaveDraft());
        binding.btnSchedule.setOnClickListener(v -> handleSchedule());
        binding.btnAddSender.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_global_addSenderFragment));
        binding.btnManageSenders.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_global_sendersFragment));
        
        binding.btnUseTemplate.setOnClickListener(v -> showTemplatePicker());
        binding.btnAiImprove.setOnClickListener(v -> showAiTonePicker());
        
        binding.toolbar.setNavigationOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());
    }

    private void showAiTonePicker() {
        String body = binding.etBody.getText().toString().trim();
        if (body.isEmpty()) {
            FeedbackUtils.showInfo(binding.getRoot(), getString(R.string.ai_improve_required));
            return;
        }
        String[] labels = {
                getString(R.string.ai_improve_professional),
                getString(R.string.ai_improve_friendly),
                getString(R.string.ai_improve_concise),
                getString(R.string.ai_improve_formal)
        };
        String[] tones = {"professional", "friendly", "concise", "formal"};
        androidx.appcompat.app.AlertDialog toneDialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.ai_improve_title)
                .setSingleChoiceItems(labels, 0, null)
                .setNegativeButton(R.string.label_cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    int selected = ((androidx.appcompat.app.AlertDialog) dialog)
                            .getListView().getCheckedItemPosition();
                    if (selected < 0 || selected >= tones.length) selected = 0;
                    improveEmail(tones[selected]);
                })
                .create();
        toneDialog.setOnShowListener(dialog -> toneDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    int selected = toneDialog.getListView().getCheckedItemPosition();
                    if (selected < 0 || selected >= tones.length) selected = 0;
                    toneDialog.dismiss();
                    improveEmail(tones[selected]);
                }));
        toneDialog.show();
    }

    private void improveEmail(String tone) {
        binding.btnAiImprove.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);
        aiComposeRepository.improve(
                binding.etSubject.getText().toString().trim(),
                binding.etBody.getText().toString().trim(),
                tone,
                new AiComposeRepository.CallbackListener() {
                    @Override
                    public void onSuccess(com.reachinbox.app.data.model.ImproveEmailResponse.Suggestion suggestion) {
                        if (!isAdded() || binding == null) return;
                        binding.btnAiImprove.setEnabled(true);
                        binding.progressBar.setVisibility(View.GONE);
                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle(R.string.ai_improve_replace_title)
                                .setMessage(getString(R.string.ai_improve_replace_message)
                                        + "\n\n" + suggestion.getSubject() + "\n\n" + suggestion.getBody())
                                .setNegativeButton(R.string.label_cancel, null)
                                .setPositiveButton(R.string.label_replace, (dialog, which) -> {
                                    binding.etSubject.setText(suggestion.getSubject());
                                    binding.etBody.setText(suggestion.getBody());
                                    FeedbackUtils.showSuccess(binding.getRoot(), getString(R.string.ai_improve_applied));
                                })
                                .show();
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded() || binding == null) return;
                        binding.btnAiImprove.setEnabled(true);
                        binding.progressBar.setVisibility(View.GONE);
                        FeedbackUtils.showError(binding.getRoot(), message);
                    }
                });
    }

    private void showTemplatePicker() {
        templateRepository.getTemplates(1, 100, null, "updatedAt", "desc", new TemplateRepository.TemplateCallback<TemplateListResponse>() {
            @Override
            public void onSuccess(TemplateListResponse result) {
                if (isAdded()) {
                    List<Template> templates = result.getTemplates();
                    if (templates.isEmpty()) {
                        FeedbackUtils.showInfo(binding.getRoot(), "No templates found. Create one in More > Templates.");
                        return;
                    }

                    String[] names = new String[templates.size()];
                    for (int i = 0; i < templates.size(); i++) {
                        names[i] = templates.get(i).getName();
                    }

                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Select Template")
                            .setItems(names, (dialog, which) -> applyTemplate(templates.get(which)))
                            .show();
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    FeedbackUtils.showError(binding.getRoot(), "Failed to load templates: " + message);
                }
            }
        });
    }

    private void applyTemplate(Template template) {
        String currentSubject = binding.etSubject.getText().toString().trim();
        String currentBody = binding.etBody.getText().toString().trim();

        if (!currentSubject.isEmpty() || !currentBody.isEmpty()) {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Replace content?")
                    .setMessage("This will replace your current subject and message. Continue?")
                    .setPositiveButton("Replace", (dialog, which) -> {
                        binding.etSubject.setText(template.getSubject());
                        binding.etBody.setText(template.getBodyText());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            binding.etSubject.setText(template.getSubject());
            binding.etBody.setText(template.getBodyText());
        }
    }

    private void setupSenderDropdown() {
        showInitialLoading(true);
        senderRepository.getSenders(new SenderRepository.SenderCallback<List<Sender>>() {
            @Override
            public void onSuccess(List<Sender> result) {
                if (isAdded()) {
                    showInitialLoading(false);
                    
                    if (result.isEmpty()) {
                        showEmptyState(true, false);
                        return;
                    }

                    senders = new ArrayList<>();
                    for (Sender s : result) {
                        if ("CONNECTED".equals(s.getStatus())) {
                            senders.add(s);
                        }
                    }

                    if (senders.isEmpty()) {
                        showEmptyState(false, true);
                        return;
                    }

                    showComposer(true);

                    List<String> displayNames = new ArrayList<>();
                    for (Sender s : senders) {
                        displayNames.add(s.getDisplayName() + " (" + s.getEmail() + ")");
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_dropdown_item_1line, displayNames);
                    binding.actvSender.setAdapter(adapter);
                    binding.actvSender.setOnItemClickListener((parent, view1, position, id) -> {
                        selectedSender = senders.get(position);
                    });
                    
                    // If we loaded a draft, select the sender in the dropdown
                    if (selectedSender != null) {
                        for (int i = 0; i < senders.size(); i++) {
                            if (senders.get(i).getId().equals(selectedSender.getId())) {
                                binding.actvSender.setText(displayNames.get(i), false);
                                break;
                            }
                        }
                    } else if (!senders.isEmpty()) {
                        // Default to first connected sender
                        selectedSender = senders.get(0);
                        binding.actvSender.setText(displayNames.get(0), false);
                    }
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    showInitialLoading(false);
                    FeedbackUtils.showError(binding.getRoot(), "Error loading senders: " + message);
                }
            }
        });
    }

    private void showInitialLoading(boolean show) {
        binding.initialLoadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            binding.composerUi.setVisibility(View.GONE);
            binding.emptyStateZeroSenders.setVisibility(View.GONE);
            binding.emptyStateNoConnected.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(boolean zeroSenders, boolean noneConnected) {
        binding.composerUi.setVisibility(View.GONE);
        binding.emptyStateZeroSenders.setVisibility(zeroSenders ? View.VISIBLE : View.GONE);
        binding.emptyStateNoConnected.setVisibility(noneConnected ? View.VISIBLE : View.GONE);
    }

    private void showComposer(boolean show) {
        binding.composerUi.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.emptyStateZeroSenders.setVisibility(View.GONE);
        binding.emptyStateNoConnected.setVisibility(View.GONE);
    }

    private void loadDraft() {
        showInitialLoading(true);
        emailRepository.getEmailDetails(emailId, new EmailRepository.EmailCallback<Email>() {
            @Override
            public void onSuccess(Email result) {
                if (isAdded()) {
                    binding.etRecipient.setText(result.getRecipient() != null ? result.getRecipient() : "");
                    binding.etSubject.setText(result.getSubject() != null ? result.getSubject() : "");
                    binding.etBody.setText(result.getTextBody() != null ? result.getTextBody() : "");
                    
                    if (result.getSenderAccount() != null) {
                        selectedSender = result.getSenderAccount();
                    }
                    
                    // Now setup dropdown to show the correct sender selected
                    setupSenderDropdown();
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    showInitialLoading(false);
                    FeedbackUtils.showError(binding.getRoot(), "Failed to load draft: " + message);
                }
            }
        });
    }

    private void handleSend() {
        if (selectedSender == null) {
            FeedbackUtils.showInfo(binding.getRoot(), getString(R.string.msg_select_sender));
            return;
        }

        String recipient = binding.etRecipient.getText().toString().trim();
        String subject = binding.etSubject.getText().toString().trim();
        String body = binding.etBody.getText().toString().trim();

        if (recipient.isEmpty() || subject.isEmpty() || body.isEmpty()) {
            FeedbackUtils.showError(binding.getRoot(), getString(R.string.error_empty_fields));
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(recipient).matches()) {
            FeedbackUtils.showError(binding.getRoot(), getString(R.string.error_invalid_email));
            return;
        }

        setLoading(true);

        if (currentIdempotencyKey == null) {
            currentIdempotencyKey = UUID.randomUUID().toString();
        }

        if (emailId != null) {
            // Send existing draft
            emailRepository.sendDraft(emailId, currentIdempotencyKey, new EmailRepository.EmailCallback<Email>() {
                @Override
                public void onSuccess(Email result) {
                    onSendSuccess();
                }

                @Override
                public void onError(String message) {
                    onSendError(message);
                }
            });
        } else {
            // Send new email
            CreateEmailRequest request = new CreateEmailRequest(selectedSender.getId(), recipient, subject, body, false, currentIdempotencyKey);
            emailRepository.sendEmail(request, new EmailRepository.EmailCallback<Email>() {
                @Override
                public void onSuccess(Email result) {
                    onSendSuccess();
                }

                @Override
                public void onError(String message) {
                    onSendError(message);
                }
            });
        }
    }

    private void onSendSuccess() {
        if (isAdded()) {
            setLoading(false);
            FeedbackUtils.showSuccess(binding.getRoot(), getString(R.string.msg_email_sent));
            Navigation.findNavController(requireView()).navigateUp();
        }
    }

    private void onSendError(String message) {
        if (isAdded()) {
            setLoading(false);
            FeedbackUtils.showError(binding.getRoot(), message);
        }
    }

    private void handleSaveDraft() {
        String recipient = binding.etRecipient.getText().toString().trim();
        String subject = binding.etSubject.getText().toString().trim();
        String body = binding.etBody.getText().toString().trim();
        String senderId = selectedSender != null ? selectedSender.getId() : null;

        if (senderId == null) {
            FeedbackUtils.showInfo(binding.getRoot(), "Please select a sender to save a draft");
            return;
        }

        setLoading(true);

        if (emailId != null) {
            // Update existing draft
            UpdateDraftRequest request = new UpdateDraftRequest();
            request.setRecipient(recipient);
            request.setSubject(subject);
            request.setTextBody(body);
            request.setSenderAccountId(senderId);

            emailRepository.updateDraft(emailId, request, new EmailRepository.EmailCallback<Email>() {
                @Override
                public void onSuccess(Email result) {
                    if (isAdded()) {
                        setLoading(false);
                        FeedbackUtils.showSuccess(binding.getRoot(), "Draft saved");
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
        } else {
            // Create new draft
            CreateDraftRequest request = new CreateDraftRequest(senderId, recipient, subject, body, false);
            emailRepository.saveDraft(request, new EmailRepository.EmailCallback<Email>() {
                @Override
                public void onSuccess(Email result) {
                    if (isAdded()) {
                        setLoading(false);
                        emailId = result.getId(); // Now we are editing this draft
                        FeedbackUtils.showSuccess(binding.getRoot(), "Draft saved");
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
    }

    private void handleSchedule() {
        final Calendar scheduledTime = Calendar.getInstance();
        
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            scheduledTime.set(Calendar.YEAR, year);
            scheduledTime.set(Calendar.MONTH, month);
            scheduledTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            new TimePickerDialog(requireContext(), (view2, hourOfDay, minute) -> {
                scheduledTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                scheduledTime.set(Calendar.MINUTE, minute);
                scheduledTime.set(Calendar.SECOND, 0);

                if (scheduledTime.getTime().before(new Date())) {
                    FeedbackUtils.showError(binding.getRoot(), getString(R.string.error_past_time));
                    return;
                }

                performSchedule(scheduledTime.getTime());
            }, scheduledTime.get(Calendar.HOUR_OF_DAY), scheduledTime.get(Calendar.MINUTE), true).show();
        }, scheduledTime.get(Calendar.YEAR), scheduledTime.get(Calendar.MONTH), scheduledTime.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void performSchedule(Date date) {
        if (selectedSender == null) {
            FeedbackUtils.showInfo(binding.getRoot(), getString(R.string.msg_select_sender));
            return;
        }

        String recipient = binding.etRecipient.getText().toString().trim();
        String subject = binding.etSubject.getText().toString().trim();
        String body = binding.etBody.getText().toString().trim();

        if (recipient.isEmpty() || subject.isEmpty() || body.isEmpty()) {
            FeedbackUtils.showError(binding.getRoot(), getString(R.string.error_empty_fields));
            return;
        }

        setLoading(true);

        SimpleDateFormat isoSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        isoSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String scheduledAt = isoSdf.format(date);

        if (emailId != null) {
            // Schedule existing draft
            emailRepository.scheduleDraft(emailId, scheduledAt, new EmailRepository.EmailCallback<Object>() {
                @Override
                public void onSuccess(Object result) {
                    onScheduleSuccess();
                }

                @Override
                public void onError(String message) {
                    onSendError(message);
                }
            });
        } else {
            // Use existing scheduling logic (could be a new endpoint or the current POST /scheduled-emails)
            // For now, I'll use the existing scheduleEmail from ScheduleEmailFragment logic
            // Wait, I should probably use EmailRepository.scheduleEmail if it existed.
            // Let me check what I implemented in Step 13.
            // In Step 13, I had ScheduledEmailRepository.
            // I should probably use that or move it to EmailRepository.
            // Let's use ScheduledEmailRepository for now to be consistent.
            
            // I'll need to instantiate it.
            new com.reachinbox.app.data.repository.ScheduledEmailRepository(requireContext())
                .scheduleEmail(new com.reachinbox.app.data.model.CreateScheduledEmailRequest(
                    selectedSender.getId(), recipient, subject, body, false, scheduledAt), 
                    new com.reachinbox.app.data.repository.ScheduledEmailRepository.ScheduledEmailCallback<com.reachinbox.app.data.model.ScheduledEmail>() {
                        @Override
                        public void onSuccess(com.reachinbox.app.data.model.ScheduledEmail result) {
                            onScheduleSuccess();
                        }

                        @Override
                        public void onError(String message) {
                            onSendError(message);
                        }
                    });
        }
    }

    private void onScheduleSuccess() {
        if (isAdded()) {
            setLoading(false);
            FeedbackUtils.showSuccess(binding.getRoot(), getString(R.string.msg_email_scheduled));
            Navigation.findNavController(requireView()).navigateUp();
        }
    }

    private void setLoading(boolean isLoading) {
        binding.btnSend.setEnabled(!isLoading);
        binding.btnSaveDraft.setEnabled(!isLoading);
        binding.btnSchedule.setEnabled(!isLoading);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
