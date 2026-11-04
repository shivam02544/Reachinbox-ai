package com.reachinbox.app.ui.followup;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.reachinbox.app.R;
import com.reachinbox.app.data.model.CreateDraftRequest;
import com.reachinbox.app.data.model.CreateEmailRequest;
import com.reachinbox.app.data.model.Email;
import com.reachinbox.app.data.model.FollowUpGenerationResult;
import com.reachinbox.app.data.model.CreateScheduledEmailRequest;
import com.reachinbox.app.data.model.ScheduledEmail;
import com.reachinbox.app.data.model.UpdateDraftRequest;
import com.reachinbox.app.data.repository.EmailRepository;
import com.reachinbox.app.data.repository.FollowUpRepository;
import com.reachinbox.app.data.repository.ScheduledEmailRepository;
import com.reachinbox.app.databinding.FragmentFollowupBinding;
import com.reachinbox.app.utils.FeedbackUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class FollowUpFragment extends Fragment {

    private FragmentFollowupBinding binding;
    private FollowUpRepository followUpRepository;
    private EmailRepository emailRepository;
    private ScheduledEmailRepository scheduledEmailRepository;

    private String parentEmailId;
    private Email parentEmail;
    
    private String generatedEmailId = null; // Will be set if saved as draft
    private int selectedFollowUpNumber = 1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            parentEmailId = getArguments().getString("parentEmailId");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFollowupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        followUpRepository = new FollowUpRepository(requireContext());
        emailRepository = new EmailRepository(requireContext());
        scheduledEmailRepository = new ScheduledEmailRepository(requireContext());

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        setupDropdowns();
        setupButtons();

        showSetup();

        if (parentEmailId != null) {
            loadParentEmailDetails();
        } else {
            showError("Missing parent email details");
        }
    }

    private void loadParentEmailDetails() {
        emailRepository.getEmailDetails(parentEmailId, new EmailRepository.EmailCallback<Email>() {
            @Override
            public void onSuccess(Email result) {
                if (isAdded()) {
                    parentEmail = result;
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    showError("Failed to load original email: " + message);
                }
            }
        });
    }

    private void setupDropdowns() {
        String[] sequences = new String[]{"Follow-Up #1 (Polite Bump)", "Follow-Up #2 (Final Wrap-up)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, sequences);
        binding.actvSequence.setAdapter(adapter);
        binding.actvSequence.setOnItemClickListener((parent, view, position, id) -> {
            selectedFollowUpNumber = position + 1;
        });
    }

    private void setupButtons() {
        binding.btnGenerate.setOnClickListener(v -> handleGenerate());
        binding.btnRegenerate.setOnClickListener(v -> {
            showSetup();
            handleGenerate();
        });
        binding.btnSaveDraft.setOnClickListener(v -> handleSaveDraft(true));
        binding.btnSend.setOnClickListener(v -> handleSend());
        binding.btnSchedule.setOnClickListener(v -> handleSchedule());
    }

    private void handleGenerate() {
        if (parentEmailId == null) {
            showError("Missing parent email");
            return;
        }

        String instructions = binding.etInstructions.getText() != null ? binding.etInstructions.getText().toString().trim() : "";

        showLoading(true);

        followUpRepository.generateFollowUp(parentEmailId, instructions, selectedFollowUpNumber, new FollowUpRepository.FollowUpCallback() {
            @Override
            public void onSuccess(FollowUpGenerationResult result) {
                showLoading(false);
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Follow-up email generated successfully", Toast.LENGTH_SHORT).show();
                }
                displayPreview(result);
            }

            @Override
            public void onError(String message) {
                showLoading(false);
                showError(message);
            }
        });
    }

    private void displayPreview(FollowUpGenerationResult result) {
        binding.layoutSetup.setVisibility(View.GONE);
        binding.layoutPreview.setVisibility(View.VISIBLE);

        binding.etSubject.setText(result.getSubject());
        binding.etBody.setText(result.getBodyText());
        
        generatedEmailId = null; // Reset draft ID for new generation
    }

    private void showSetup() {
        binding.layoutSetup.setVisibility(View.VISIBLE);
        binding.layoutPreview.setVisibility(View.GONE);
    }

    private void handleSaveDraft(boolean showToast) {
        if (parentEmail == null || parentEmail.getSenderAccount() == null) {
            showError("Sender details not loaded");
            return;
        }
        
        String subject = binding.etSubject.getText().toString().trim();
        String body = binding.etBody.getText().toString().trim();

        if (subject.isEmpty() || body.isEmpty()) {
            showError("Subject and body cannot be empty");
            return;
        }

        setButtonsEnabled(false);

        if (generatedEmailId != null) {
            // update
            UpdateDraftRequest req = new UpdateDraftRequest();
            req.setRecipient(parentEmail.getRecipient());
            req.setSubject(subject);
            req.setTextBody(body);
            req.setSenderAccountId(parentEmail.getSenderAccount().getId());
            req.setParentEmailId(parentEmailId);
            req.setFollowUpNumber(selectedFollowUpNumber);

            emailRepository.updateDraft(generatedEmailId, req, new EmailRepository.EmailCallback<Email>() {
                @Override
                public void onSuccess(Email result) {
                    setButtonsEnabled(true);
                    if (showToast) FeedbackUtils.showSuccess(binding.getRoot(), "Follow-up draft saved");
                }

                @Override
                public void onError(String message) {
                    setButtonsEnabled(true);
                    showError(message);
                }
            });
        } else {
            // Since EmailRepository / CreateDraftRequest doesn't currently accept parentEmailId and followUpNumber in Android DTO
            // Wait, we didn't add parentEmailId and followUpNumber to CreateDraftRequest?
            // Actually I should update CreateDraftRequest and CreateEmailRequest to send parentEmailId.
            // But if I can't send them now, I can just rely on the fact that Phase 5 backend accepts them.
            // Let me update CreateDraftRequest fields using reflections or just update the class.
            // I'll update CreateEmailRequest to include those fields momentarily.
            
            CreateDraftRequest req = new CreateDraftRequest(parentEmail.getSenderAccount().getId(), parentEmail.getRecipient(), subject, body, false);
            req.setParentEmailId(parentEmailId);
            req.setFollowUpNumber(selectedFollowUpNumber);
            
            emailRepository.saveDraft(req, new EmailRepository.EmailCallback<Email>() {
                @Override
                public void onSuccess(Email result) {
                    setButtonsEnabled(true);
                    generatedEmailId = result.getId();
                    if (showToast) FeedbackUtils.showSuccess(binding.getRoot(), "Follow-up draft saved");
                }

                @Override
                public void onError(String message) {
                    setButtonsEnabled(true);
                    showError(message);
                }
            });
        }
    }

    private void handleSend() {
        if (parentEmail == null || parentEmail.getSenderAccount() == null) return;

        String subject = binding.etSubject.getText().toString().trim();
        String body = binding.etBody.getText().toString().trim();

        if (subject.isEmpty() || body.isEmpty()) {
            showError("Subject and body cannot be empty");
            return;
        }

        setButtonsEnabled(false);
        String idempotencyKey = UUID.randomUUID().toString();

        if (generatedEmailId != null) {
            emailRepository.sendDraft(generatedEmailId, idempotencyKey, new EmailRepository.EmailCallback<Email>() {
                @Override
                public void onSuccess(Email result) {
                    onSuccessAction("Follow-up sent");
                }
                @Override
                public void onError(String message) {
                    setButtonsEnabled(true);
                    showError(message);
                }
            });
        } else {
            CreateEmailRequest req = new CreateEmailRequest(parentEmail.getSenderAccount().getId(), parentEmail.getRecipient(), subject, body, false, idempotencyKey);
            req.setParentEmailId(parentEmailId);
            req.setFollowUpNumber(selectedFollowUpNumber);
            
            emailRepository.sendEmail(req, new EmailRepository.EmailCallback<Email>() {
                @Override
                public void onSuccess(Email result) {
                    onSuccessAction("Follow-up sent");
                }
                @Override
                public void onError(String message) {
                    setButtonsEnabled(true);
                    showError(message);
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
                    showError("Scheduled time must be in the future");
                    return;
                }

                performSchedule(scheduledTime.getTime());
            }, scheduledTime.get(Calendar.HOUR_OF_DAY), scheduledTime.get(Calendar.MINUTE), true).show();
        }, scheduledTime.get(Calendar.YEAR), scheduledTime.get(Calendar.MONTH), scheduledTime.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void performSchedule(Date date) {
        if (parentEmail == null || parentEmail.getSenderAccount() == null) return;
        
        String subject = binding.etSubject.getText().toString().trim();
        String body = binding.etBody.getText().toString().trim();

        setButtonsEnabled(false);

        SimpleDateFormat isoSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        isoSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String scheduledAt = isoSdf.format(date);

        if (generatedEmailId != null) {
            emailRepository.scheduleDraft(generatedEmailId, scheduledAt, new EmailRepository.EmailCallback<Object>() {
                @Override
                public void onSuccess(Object result) {
                    onSuccessAction("Follow-up scheduled");
                }
                @Override
                public void onError(String message) {
                    setButtonsEnabled(true);
                    showError(message);
                }
            });
        } else {
            CreateScheduledEmailRequest request = new CreateScheduledEmailRequest(parentEmail.getSenderAccount().getId(), parentEmail.getRecipient(), subject, body, false, scheduledAt);
            request.setParentEmailId(parentEmailId);
            request.setFollowUpNumber(selectedFollowUpNumber);
            
            scheduledEmailRepository.scheduleEmail(request, new ScheduledEmailRepository.ScheduledEmailCallback<ScheduledEmail>() {
                @Override
                public void onSuccess(ScheduledEmail result) {
                    onSuccessAction("Follow-up scheduled");
                }
                @Override
                public void onError(String message) {
                    setButtonsEnabled(true);
                    showError(message);
                }
            });
        }
    }

    private void onSuccessAction(String msg) {
        if (isAdded()) {
            FeedbackUtils.showSuccess(binding.getRoot(), msg);
            Navigation.findNavController(requireView()).navigateUp();
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        binding.btnSend.setEnabled(enabled);
        binding.btnSaveDraft.setEnabled(enabled);
        binding.btnSchedule.setEnabled(enabled);
        binding.btnRegenerate.setEnabled(enabled);
    }

    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.tvStatus.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.layoutContent.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
