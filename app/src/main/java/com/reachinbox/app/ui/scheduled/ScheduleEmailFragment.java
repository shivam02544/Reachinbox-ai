package com.reachinbox.app.ui.scheduled;

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
import com.reachinbox.app.data.model.CreateScheduledEmailRequest;
import com.reachinbox.app.data.model.ScheduledEmail;
import com.reachinbox.app.data.model.Sender;
import com.reachinbox.app.data.repository.ScheduledEmailRepository;
import com.reachinbox.app.data.repository.SenderRepository;
import com.reachinbox.app.databinding.FragmentScheduleEmailBinding;
import com.reachinbox.app.utils.FeedbackUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ScheduleEmailFragment extends Fragment {

    private FragmentScheduleEmailBinding binding;
    private ScheduledEmailRepository scheduledEmailRepository;
    private SenderRepository senderRepository;
    private List<Sender> senders = new ArrayList<>();
    private Sender selectedSender;
    private Calendar selectedDateTime = Calendar.getInstance();
    private boolean isDateTimeSelected = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentScheduleEmailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scheduledEmailRepository = new ScheduledEmailRepository(requireContext());
        senderRepository = new SenderRepository(requireContext());

        setupSenderDropdown();
        setupDateTimePickers();
        binding.btnSchedule.setOnClickListener(v -> handleSchedule());
    }

    private void setupSenderDropdown() {
        senderRepository.getSenders(new SenderRepository.SenderCallback<>() {
            @Override
            public void onSuccess(List<Sender> result) {
                if (isAdded()) {
                    senders = new ArrayList<>();
                    for (Sender s : result) {
                        if ("CONNECTED".equals(s.getStatus())) {
                            senders.add(s);
                        }
                    }

                    if (senders.isEmpty()) {
                        FeedbackUtils.showInfo(binding.getRoot(), "No connected senders found.");
                        Navigation.findNavController(requireView()).navigateUp();
                        return;
                    }

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
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    FeedbackUtils.showError(binding.getRoot(), "Error: " + message);
                }
            }
        });
    }

    private void setupDateTimePickers() {
        binding.btnDatePicker.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                selectedDateTime.set(Calendar.YEAR, year);
                selectedDateTime.set(Calendar.MONTH, month);
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateSelectedDateTimeText();
            }, selectedDateTime.get(Calendar.YEAR), selectedDateTime.get(Calendar.MONTH), 
               selectedDateTime.get(Calendar.DAY_OF_MONTH)).show();
        });

        binding.btnTimePicker.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDateTime.set(Calendar.MINUTE, minute);
                selectedDateTime.set(Calendar.SECOND, 0);
                isDateTimeSelected = true;
                updateSelectedDateTimeText();
            }, selectedDateTime.get(Calendar.HOUR_OF_DAY), selectedDateTime.get(Calendar.MINUTE), true).show();
        });
    }

    private void updateSelectedDateTimeText() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        binding.tvSelectedDateTime.setText(sdf.format(selectedDateTime.getTime()));
    }

    private void handleSchedule() {
        if (selectedSender == null) {
            FeedbackUtils.showError(binding.getRoot(), getString(R.string.msg_select_sender));
            return;
        }

        if (!isDateTimeSelected || selectedDateTime.getTime().before(new Date())) {
            FeedbackUtils.showError(binding.getRoot(), getString(R.string.error_past_time));
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

        // Convert to ISO-8601 UTC
        SimpleDateFormat isoSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        isoSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String scheduledAt = isoSdf.format(selectedDateTime.getTime());

        CreateScheduledEmailRequest request = new CreateScheduledEmailRequest(
                selectedSender.getId(), recipient, subject, body, false, scheduledAt);

        scheduledEmailRepository.scheduleEmail(request, new ScheduledEmailRepository.ScheduledEmailCallback<>() {
            @Override
            public void onSuccess(ScheduledEmail result) {
                if (isAdded()) {
                    setLoading(false);
                    FeedbackUtils.showSuccess(binding.getRoot(), getString(R.string.msg_email_scheduled));
                    Navigation.findNavController(requireView()).navigateUp();
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

    private void setLoading(boolean isLoading) {
        binding.btnSchedule.setEnabled(!isLoading);
        binding.btnSchedule.setText(isLoading ? "" : getString(R.string.btn_schedule_email));
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
