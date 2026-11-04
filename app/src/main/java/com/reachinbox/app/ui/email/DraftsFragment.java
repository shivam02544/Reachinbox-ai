package com.reachinbox.app.ui.email;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.reachinbox.app.R;
import com.reachinbox.app.adapter.EmailAdapter;
import com.reachinbox.app.data.model.Email;
import com.reachinbox.app.data.model.EmailListResponse;
import com.reachinbox.app.data.repository.EmailRepository;
import com.reachinbox.app.databinding.FragmentDraftsBinding;
import com.reachinbox.app.utils.FeedbackUtils;

import java.util.List;

public class DraftsFragment extends Fragment implements EmailAdapter.OnEmailClickListener {

    private FragmentDraftsBinding binding;
    private EmailRepository emailRepository;
    private EmailAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDraftsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emailRepository = new EmailRepository(requireContext());
        setupRecyclerView();
        setupSwipeRefresh();

        loadDrafts();
    }

    private void setupRecyclerView() {
        adapter = new EmailAdapter(this);
        binding.rvDrafts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvDrafts.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(this::loadDrafts);
    }

    private void loadDrafts() {
        if (binding == null) return;
        binding.swipeRefresh.setRefreshing(true);
        emailRepository.getDrafts(1, 100, new EmailRepository.EmailCallback<EmailListResponse>() {
            @Override
            public void onSuccess(EmailListResponse result) {
                if (isAdded() && binding != null) {
                    binding.swipeRefresh.setRefreshing(false);
                    updateUI(result.getEmails());
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded() && binding != null) {
                    binding.swipeRefresh.setRefreshing(false);
                    FeedbackUtils.showError(binding.getRoot(), message);
                }
            }
        });
    }

    private void updateUI(List<Email> emails) {
        if (binding == null) return;
        if (emails == null || emails.isEmpty()) {
            binding.rvDrafts.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.VISIBLE);
        } else {
            adapter.setEmails(emails);
            binding.rvDrafts.setVisibility(View.VISIBLE);
            binding.emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onEmailClick(Email email) {
        if (email.getId() == null) return;
        Bundle args = new Bundle();
        args.putString("emailId", email.getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_draftsFragment_to_composeEmailFragment, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
