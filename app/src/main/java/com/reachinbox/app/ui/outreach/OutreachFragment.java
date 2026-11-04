package com.reachinbox.app.ui.outreach;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Patterns;
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
import com.reachinbox.app.data.model.GenerateOutreachRequest;
import com.reachinbox.app.data.model.OutreachGenerationResult;
import com.reachinbox.app.data.model.Sender;
import com.reachinbox.app.data.model.CreateScheduledEmailRequest;
import com.reachinbox.app.data.model.ScheduledEmail;
import com.reachinbox.app.data.repository.EmailRepository;
import com.reachinbox.app.data.repository.OutreachRepository;
import com.reachinbox.app.data.repository.SenderRepository;
import com.reachinbox.app.data.repository.ScheduledEmailRepository;
import com.reachinbox.app.databinding.FragmentOutreachBinding;
import com.reachinbox.app.utils.FeedbackUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class OutreachFragment extends Fragment {

    private FragmentOutreachBinding binding;
    private OutreachRepository outreachRepository;
    private SenderRepository senderRepository;
    private EmailRepository emailRepository;
    private ScheduledEmailRepository scheduledEmailRepository;

    private List<Sender> senders = new ArrayList<>();
    private Sender selectedSender;
    private String jobRequirementId;
    
    private String generatedEmailId = null; // Will be set if saved as draft

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            jobRequirementId = getArguments().getString("jobRequirementId");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOutreachBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        outreachRepository = new OutreachRepository(requireContext());
        senderRepository = new SenderRepository(requireContext());
        emailRepository = new EmailRepository(requireContext());
        scheduledEmailRepository = new ScheduledEmailRepository(requireContext());

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        setupDropdowns();
        setupButtons();

        // Default to setup state
        showSetup();
    }

    private void setupDropdowns() {
        // Tones
        String[] tones = new String[]{"professional", "concise", "friendly_professional", "formal"};
        ArrayAdapter<String> toneAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, tones);
        binding.actvTone.setAdapter(toneAdapter);

        // Senders
        senderRepository.getSenders(new SenderRepository.SenderCallback<List<Sender>>() {
            @Override
            public void onSuccess(List<Sender> result) {
                if (isAdded()) {
                    senders = new ArrayList<>();
                    for (Sender s : result) {
                        if ("CONNECTED".equals(s.getStatus())) {
                            senders.add(s);
                        }
                    }

                    List<String> displayNames = new ArrayList<>();
                    for (Sender s : senders) {
                        displayNames.add(s.getDisplayName() + " (" + s.getEmail() + ")");
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, displayNames);
                    binding.actvSender.setAdapter(adapter);
                    binding.actvSender.setOnItemClickListener((parent, view1, position, id) -> selectedSender = senders.get(position));
                    
                    if (!senders.isEmpty()) {
                        selectedSender = senders.get(0);
                        binding.actvSender.setText(displayNames.get(0), false);
                    }
                }
            }

            @Override
            public void onError(String message) {
                showError("Failed to load senders: " + message);
            }
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
        if (jobRequirementId == null) {
            showError("Missing Job Requirement");
            return;
        }

        String email = binding.etRecipientEmail.getText() != null ? binding.etRecipientEmail.getText().toString().trim() : "";
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etRecipientEmail.setError("Valid email required");
            return;
        }

        if (selectedSender == null) {
            showError("Please select a sender account");
            return;
        }

        String name = binding.etRecipientName.getText() != null ? binding.etRecipientName.getText().toString().trim() : "";
        String company = binding.etRecipientCompany.getText() != null ? binding.etRecipientCompany.getText().toString().trim() : "";
        String role = binding.etRecipientRole.getText() != null ? binding.etRecipientRole.getText().toString().trim() : "";
        String instructions = binding.etInstructions.getText() != null ? binding.etInstructions.getText().toString().trim() : "";
        String tone = binding.actvTone.getText().toString();

        GenerateOutreachRequest.Recipient recipient = new GenerateOutreachRequest.Recipient(name, email, company, role);

        showLoading(true);

        outreachRepository.generateOutreach(jobRequirementId, recipient, instructions, tone, new OutreachRepository.OutreachCallback() {
            @Override
            public void onSuccess(OutreachGenerationResult result) {
                showLoading(false);
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Outreach email generated successfully", Toast.LENGTH_SHORT).show();
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

    private void displayPreview(OutreachGenerationResult result) {
        binding.layoutSetup.setVisibility(View.GONE);
        binding.layoutPreview.setVisibility(View.VISIBLE);

        binding.etSubject.setText(result.getSubject());
        binding.etBody.setText(result.getBodyText());

        if (result.getPersonalizationPoints() != null && !result.getPersonalizationPoints().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String point : result.getPersonalizationPoints()) {
                sb.append("• ").append(point).append("\n");
            }
            binding.tvPersonalizationPoints.setText(sb.toString().trim());
        } else {
            binding.tvPersonalizationPoints.setText("No specific personalization points.");
        }
        
        generatedEmailId = null; // Reset draft ID for new generation
    }

    private void showSetup() {
        binding.layoutSetup.setVisibility(View.VISIBLE);
        binding.layoutPreview.setVisibility(View.GONE);
    }

    private void handleSaveDraft(boolean showToast) {
        if (selectedSender == null) return;
        
        String recipient = binding.etRecipientEmail.getText().toString().trim();
        String subject = binding.etSubject.getText().toString().trim();
        String body = binding.etBody.getText().toString().trim();

        if (subject.isEmpty() || body.isEmpty()) {
            showError("Subject and body cannot be empty");
            return;
        }

        setButtonsEnabled(false);

        if (generatedEmailId != null) {
            // update
            com.reachinbox.app.data.model.UpdateDraftRequest req = new com.reachinbox.app.data.model.UpdateDraftRequest();
            req.setRecipient(recipient);
            req.setSubject(subject);
            req.setTextBody(body);
            req.setSenderAccountId(selectedSender.getId());

            emailRepository.updateDraft(generatedEmailId, req, new EmailRepository.EmailCallback<Email>() {
                @Override
                public void onSuccess(Email result) {
                    setButtonsEnabled(true);
                    if (showToast) FeedbackUtils.showSuccess(binding.getRoot(), "Draft saved");
                }

                @Override
                public void onError(String message) {
                    setButtonsEnabled(true);
                    showError(message);
                }
            });
        } else {
            CreateDraftRequest req = new CreateDraftRequest(selectedSender.getId(), recipient, subject, body, false);
            emailRepository.saveDraft(req, new EmailRepository.EmailCallback<Email>() {
                @Override
                public void onSuccess(Email result) {
                    setButtonsEnabled(true);
                    generatedEmailId = result.getId();
                    if (showToast) FeedbackUtils.showSuccess(binding.getRoot(), "Draft saved");
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
        if (selectedSender == null) return;

        String recipient = binding.etRecipientEmail.getText().toString().trim();
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
                    onSuccessAction("Email sent");
                }
                @Override
                public void onError(String message) {
                    setButtonsEnabled(true);
                    showError(message);
                }
            });
        } else {
            CreateEmailRequest req = new CreateEmailRequest(selectedSender.getId(), recipient, subject, body, false, idempotencyKey);
            emailRepository.sendEmail(req, new EmailRepository.EmailCallback<Email>() {
                @Override
                public void onSuccess(Email result) {
                    onSuccessAction("Email sent");
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
        if (selectedSender == null) return;
        
        String recipient = binding.etRecipientEmail.getText().toString().trim();
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
                    onSuccessAction("Email scheduled");
                }
                @Override
                public void onError(String message) {
                    setButtonsEnabled(true);
                    showError(message);
                }
            });
        } else {
            CreateScheduledEmailRequest request = new CreateScheduledEmailRequest(selectedSender.getId(), recipient, subject, body, false, scheduledAt);
            scheduledEmailRepository.scheduleEmail(request, new ScheduledEmailRepository.ScheduledEmailCallback<ScheduledEmail>() {
                @Override
                public void onSuccess(ScheduledEmail result) {
                    onSuccessAction("Email scheduled");
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
