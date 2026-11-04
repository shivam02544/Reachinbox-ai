package com.reachinbox.app.ui.senders;

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
import com.reachinbox.app.adapter.SenderAdapter;
import com.reachinbox.app.data.model.Sender;
import com.reachinbox.app.data.repository.SenderRepository;
import com.reachinbox.app.databinding.FragmentSendersBinding;
import com.reachinbox.app.utils.DialogHelper;
import com.reachinbox.app.utils.FeedbackUtils;

import java.util.List;

public class SendersFragment extends Fragment implements SenderAdapter.OnSenderActionListener {

    private FragmentSendersBinding binding;
    private SenderRepository senderRepository;
    private SenderAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSendersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        senderRepository = new SenderRepository(requireContext());
        setupRecyclerView();
        setupSwipeRefresh();

        binding.fabAddSender.setOnClickListener(v -> navigateToAddSender());
        binding.btnAddSenderEmpty.setOnClickListener(v -> navigateToAddSender());

        loadSenders();
    }

    private void navigateToAddSender() {
        Navigation.findNavController(requireView()).navigate(R.id.action_sendersFragment_to_addSenderFragment);
    }

    private void setupRecyclerView() {
        adapter = new SenderAdapter(this);
        binding.rvSenders.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvSenders.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(this::loadSenders);
    }

    private void loadSenders() {
        binding.swipeRefresh.setRefreshing(true);
        senderRepository.getSenders(new SenderRepository.SenderCallback<>() {
            @Override
            public void onSuccess(List<Sender> result) {
                if (isAdded()) {
                    binding.swipeRefresh.setRefreshing(false);
                    updateUI(result);
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    binding.swipeRefresh.setRefreshing(false);
                    FeedbackUtils.showError(binding.getRoot(), message);
                }
            }
        });
    }

    private void updateUI(List<Sender> senders) {
        if (senders.isEmpty()) {
            binding.rvSenders.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.VISIBLE);
        } else {
            adapter.setSenders(senders);
            binding.rvSenders.setVisibility(View.VISIBLE);
            binding.emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onVerify(Sender sender) {
        binding.swipeRefresh.setRefreshing(true);
        senderRepository.verifySender(sender.getId(), new SenderRepository.SenderCallback<>() {
            @Override
            public void onSuccess(Sender result) {
                if (isAdded()) {
                    loadSenders(); // Reload to show updated status
                    if ("CONNECTED".equals(result.getStatus())) {
                        FeedbackUtils.showSuccess(binding.getRoot(), "Sender verified!");
                    } else {
                        FeedbackUtils.showError(binding.getRoot(), getString(R.string.msg_sender_verify_fail));
                    }
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    binding.swipeRefresh.setRefreshing(false);
                    FeedbackUtils.showError(binding.getRoot(), message);
                }
            }
        });
    }

    @Override
    public void onDelete(Sender sender) {
        DialogHelper.showDestructiveConfirmation(requireContext(),
                "Delete Sender",
                "Are you sure you want to delete this sender?",
                "Delete",
                new DialogHelper.DialogCallback() {
                    @Override
                    public void onPositive() {
                        performDelete(sender.getId());
                    }
                });
    }

    private void performDelete(String id) {
        binding.swipeRefresh.setRefreshing(true);
        senderRepository.deleteSender(id, new SenderRepository.SenderCallback<>() {
            @Override
            public void onSuccess(Void result) {
                if (isAdded()) {
                    loadSenders();
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    binding.swipeRefresh.setRefreshing(false);
                    FeedbackUtils.showError(binding.getRoot(), message);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
