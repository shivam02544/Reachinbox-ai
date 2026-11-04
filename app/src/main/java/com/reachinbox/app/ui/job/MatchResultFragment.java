package com.reachinbox.app.ui.job;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.reachinbox.app.data.model.MatchAnalysis;
import com.reachinbox.app.data.model.MatchAnalysisResult;
import com.reachinbox.app.data.repository.MatchRepository;
import com.reachinbox.app.databinding.FragmentMatchResultBinding;

public class MatchResultFragment extends Fragment {

    private FragmentMatchResultBinding binding;
    private MatchRepository repository;
    private String jobRequirementId;
    private String suggestedSendAt;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            jobRequirementId = getArguments().getString("jobRequirementId");
            suggestedSendAt = getArguments().getString("suggestedSendAt");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMatchResultBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new MatchRepository(requireContext());

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
        
        if (suggestedSendAt != null && !suggestedSendAt.isEmpty()) {
            binding.tvSuggestedSend.setVisibility(View.VISIBLE);
            binding.tvSuggestedSend.setText("Suggested send time: " + suggestedSendAt);
        } else {
            binding.tvSuggestedSend.setVisibility(View.GONE);
        }

        binding.btnGenerateEmail.setOnClickListener(v -> {
            if (jobRequirementId != null) {
                Bundle args = new Bundle();
                args.putString("jobRequirementId", jobRequirementId);
                if (suggestedSendAt != null) {
                    args.putString("suggestedSendAt", suggestedSendAt);
                }
                androidx.navigation.Navigation.findNavController(getView()).navigate(com.reachinbox.app.R.id.action_matchResultFragment_to_outreachFragment, args);
            }
        });

        if (jobRequirementId != null) {
            generateAndLoadMatch();
        } else {
            showError("Invalid Job Requirement");
        }
    }

    private void generateAndLoadMatch() {
        showLoading(true);

        repository.generateMatch(jobRequirementId, new MatchRepository.MatchCallback() {
            @Override
            public void onSuccess(MatchAnalysis matchAnalysis) {
                showLoading(false);
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Match analysis generated successfully", Toast.LENGTH_SHORT).show();
                }
                displayMatchResult(matchAnalysis.getResult());
            }

            @Override
            public void onError(String message) {
                showLoading(false);
                showError(message);
            }
        });
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

    private void displayMatchResult(MatchAnalysisResult result) {
        if (result == null) return;

        binding.tvPersonalizationPoints.setText(formatList(result.getPersonalizationPoints(), "No specific points identified."));
        binding.tvMatchingSkills.setText(formatList(result.getMatchingSkills(), "None"));
        binding.tvRelevantExperience.setText(formatList(result.getRelevantExperience(), "None explicitly mentioned"));
        binding.tvMissingSkills.setText(formatList(result.getMissingSkills(), "None"));
        binding.tvPotentialGaps.setText(formatList(result.getPotentialGaps(), "None"));
    }

    private String formatList(java.util.List<String> list, String fallback) {
        if (list == null || list.isEmpty()) return fallback;
        StringBuilder sb = new StringBuilder();
        for (String item : list) {
            sb.append("• ").append(item).append("\n");
        }
        return sb.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
