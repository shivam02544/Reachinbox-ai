package com.reachinbox.app.ui.job;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.reachinbox.app.R;
import com.reachinbox.app.data.model.GenerateOutreachRequest;
import com.reachinbox.app.data.model.JobFromUrlResult;
import com.reachinbox.app.data.model.JobRequirement;
import com.reachinbox.app.data.repository.JobRepository;
import com.reachinbox.app.databinding.FragmentJobRequirementBinding;

public class JobRequirementFragment extends Fragment {

    private FragmentJobRequirementBinding binding;
    private JobRepository repository;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentJobRequirementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new JobRepository(requireContext());

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        binding.btnAnalyze.setOnClickListener(v -> handleAnalyze());
        binding.btnFromUrl.setOnClickListener(v -> handleFromUrl());
    }

    private void handleAnalyze() {
        String description = binding.etDescription.getText() != null ? binding.etDescription.getText().toString().trim() : "";
        if (description.isEmpty()) {
            binding.etDescription.setError("Job description is required");
            return;
        }
        if (description.length() < 20) {
            binding.etDescription.setError("Job description is too short (min 20 characters)");
            return;
        }

        String title = binding.etJobTitle.getText() != null ? binding.etJobTitle.getText().toString().trim() : "";
        String company = binding.etCompany.getText() != null ? binding.etCompany.getText().toString().trim() : "";

        showLoading(true, "Analyzing Job...");

        repository.createJobRequirement(title, company, description, new JobRepository.JobCallback() {
            @Override
            public void onSuccess(JobRequirement jobRequirement) {
                showLoading(false, null);
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Job requirement analyzed successfully", Toast.LENGTH_SHORT).show();
                }
                navigateToMatchResult(jobRequirement.getId(), null);
            }

            @Override
            public void onError(String message) {
                showLoading(false, null);
                showError(message);
            }
        });
    }

    private void handleFromUrl() {
        String url = binding.etJobUrl.getText() != null ? binding.etJobUrl.getText().toString().trim() : "";
        if (url.isEmpty()) {
            binding.etJobUrl.setError("Job posting URL is required");
            return;
        }
        if (!Patterns.WEB_URL.matcher(url).matches()) {
            binding.etJobUrl.setError("Enter a valid http(s) URL");
            return;
        }

        String recipientEmail = binding.etRecipientEmail.getText() != null
                ? binding.etRecipientEmail.getText().toString().trim()
                : "";
        GenerateOutreachRequest.Recipient recipient = null;
        if (!recipientEmail.isEmpty()) {
            if (!Patterns.EMAIL_ADDRESS.matcher(recipientEmail).matches()) {
                binding.etRecipientEmail.setError("Enter a valid recipient email");
                return;
            }
            String company = binding.etCompany.getText() != null ? binding.etCompany.getText().toString().trim() : "";
            recipient = new GenerateOutreachRequest.Recipient(null, recipientEmail, company, null);
        }

        showLoading(true, "Importing job URL and matching resume...");

        repository.createFromUrl(url, recipient, "professional", new JobRepository.JobFromUrlCallback() {
            @Override
            public void onSuccess(JobFromUrlResult result) {
                showLoading(false, null);
                if (result.getJobRequirement() == null || result.getJobRequirement().getId() == null) {
                    showError("Import succeeded but job id was missing");
                    return;
                }
                if (result.getSuggestedSendAt() != null && getContext() != null) {
                    Toast.makeText(
                            getContext(),
                            "Suggested send time: " + result.getSuggestedSendAt(),
                            Toast.LENGTH_LONG
                    ).show();
                }
                navigateToMatchResult(result.getJobRequirement().getId(), result.getSuggestedSendAt());
            }

            @Override
            public void onError(String message) {
                showLoading(false, null);
                showError(message);
            }
        });
    }

    private void showLoading(boolean isLoading, String status) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.tvStatus.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (status != null) {
            binding.tvStatus.setText(status);
        }
        binding.layoutContent.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    private void navigateToMatchResult(String jobRequirementId, String suggestedSendAt) {
        if (getView() != null) {
            Bundle args = new Bundle();
            args.putString("jobRequirementId", jobRequirementId);
            if (suggestedSendAt != null) {
                args.putString("suggestedSendAt", suggestedSendAt);
            }
            Navigation.findNavController(getView()).navigate(R.id.matchResultFragment, args);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
