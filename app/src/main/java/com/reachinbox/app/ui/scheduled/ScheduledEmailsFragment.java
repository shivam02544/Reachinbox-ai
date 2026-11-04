package com.reachinbox.app.ui.scheduled;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.reachinbox.app.R;
import com.reachinbox.app.adapter.EmailAdapter;
import com.reachinbox.app.data.model.Email;
import com.reachinbox.app.data.model.EmailListResponse;
import com.reachinbox.app.data.repository.EmailRepository;
import com.reachinbox.app.data.repository.SenderRepository;
import com.reachinbox.app.databinding.FragmentScheduledEmailsBinding;
import com.reachinbox.app.utils.FeedbackUtils;

import java.util.List;
import java.util.ArrayList;

public class ScheduledEmailsFragment extends Fragment implements EmailAdapter.OnEmailClickListener {

    private FragmentScheduledEmailsBinding binding;
    private EmailRepository emailRepository;
    private EmailAdapter adapter;

    private int currentPage = 1;
    private boolean isLoadingNextPage = false;
    private boolean hasNextPage = true;
    private String currentSearch = null;
    private String currentStatus = null;
    private String currentSenderAccountId = null;
    private String currentSortBy = "createdAt";
    private String currentSortOrder = "desc";
    private List<Email> allEmails = new ArrayList<>();

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentScheduledEmailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emailRepository = new EmailRepository(requireContext());
        setupRecyclerView();
        setupSwipeRefresh();
        setupSearch();
        setupFilters();
        setupSorting();
        loadSenderFilter();

        binding.fabCompose.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_scheduledEmailsFragment_to_composeEmailFragment)
        );
        binding.btnComposeEmpty.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_scheduledEmailsFragment_to_composeEmailFragment)
        );

        loadEmails(true);
    }

    private void loadSenderFilter() {
        new SenderRepository(requireContext()).getSenders(new SenderRepository.SenderCallback<List<com.reachinbox.app.data.model.Sender>>() {
            @Override
            public void onSuccess(List<com.reachinbox.app.data.model.Sender> senders) {
                if (!isAdded() || binding == null || senders == null || senders.isEmpty()) return;
                List<String> labels = new ArrayList<>();
                List<String> ids = new ArrayList<>();
                labels.add(getString(R.string.filter_all_senders));
                ids.add(null);
                for (com.reachinbox.app.data.model.Sender sender : senders) {
                    labels.add(sender.getDisplayName() + " · " + sender.getEmail());
                    ids.add(sender.getId());
                }
                ArrayAdapter<String> senderAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, labels);
                binding.actvSenderFilter.setAdapter(senderAdapter);
                binding.actvSenderFilter.setText(labels.get(0), false);
                binding.tilSenderFilter.setVisibility(View.VISIBLE);
                binding.actvSenderFilter.setOnItemClickListener((parent, row, position, id) -> {
                    currentSenderAccountId = ids.get(position);
                    loadEmails(true);
                });
            }

            @Override
            public void onError(String message) {
                // Sender filtering is optional; email list remains available if senders fail to load.
            }
        });
    }

    private void setupSorting() {
        binding.btnSortEmails.setOnClickListener(v -> {
            String[] labels = {
                    getString(R.string.sort_newest), getString(R.string.sort_oldest),
                    getString(R.string.sort_recipient_az), getString(R.string.sort_subject_az)
            };
            String[] fields = {"createdAt", "createdAt", "recipient", "subject"};
            String[] orders = {"desc", "asc", "asc", "asc"};
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.sort_emails_title)
                    .setItems(labels, (dialog, which) -> {
                        currentSortBy = fields[which];
                        currentSortOrder = orders[which];
                        binding.btnSortEmails.setText(labels[which]);
                        loadEmails(true);
                    }).show();
        });
    }

    private void setupRecyclerView() {
        adapter = new EmailAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.rvEmails.setLayoutManager(layoutManager);
        binding.rvEmails.setAdapter(adapter);

        binding.rvEmails.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoadingNextPage && hasNextPage) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                            loadEmails(false);
                        }
                    }
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> loadEmails(true));
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchRunnable = () -> {
                    String query = s.toString().trim();
                    currentSearch = query.isEmpty() ? null : query;
                    loadEmails(true);
                };
                searchHandler.postDelayed(searchRunnable, 500);
            }
        });
    }

    private void setupFilters() {
        binding.cgFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentStatus = null;
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chipSent) currentStatus = "SENT";
                else if (id == R.id.chipScheduled) currentStatus = "SCHEDULED";
                else if (id == R.id.chipFailed) currentStatus = "FAILED";
                else currentStatus = null;
            }
            loadEmails(true);
        });
    }

    private void loadEmails(boolean reset) {
        if (binding == null) return;

        if (reset) {
            currentPage = 1;
            hasNextPage = true;
            allEmails.clear();
            isLoadingNextPage = false;
            binding.swipeRefresh.setRefreshing(true);
        }

        if (isLoadingNextPage) return;
        isLoadingNextPage = true;
        
        emailRepository.getEmails(currentPage, 20, currentSearch, currentStatus, currentSenderAccountId, null, null, currentSortBy, currentSortOrder,
            new EmailRepository.EmailCallback<EmailListResponse>() {
                @Override
                public void onSuccess(EmailListResponse result) {
                    if (isAdded() && binding != null) {
                        binding.swipeRefresh.setRefreshing(false);
                        isLoadingNextPage = false;
                        
                        allEmails.addAll(result.getEmails());
                        adapter.setEmails(allEmails);
                        
                        hasNextPage = result.getPagination().hasNextPage();
                        if (hasNextPage) currentPage++;

                        updateUI();
                    }
                }

                @Override
                public void onError(String message) {
                    if (isAdded() && binding != null) {
                        binding.swipeRefresh.setRefreshing(false);
                        isLoadingNextPage = false;
                        FeedbackUtils.showError(binding.getRoot(), message);
                    }
                }
            });
    }

    private void updateUI() {
        if (binding == null) return;

        if (allEmails.isEmpty()) {
            binding.rvEmails.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.VISIBLE);
        } else {
            binding.rvEmails.setVisibility(View.VISIBLE);
            binding.emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onEmailClick(Email email) {
        Bundle args = new Bundle();
        args.putString("emailId", email.getId());
        
        if ("DRAFT".equals(email.getStatus())) {
            Navigation.findNavController(requireView()).navigate(R.id.action_scheduledEmailsFragment_to_composeEmailFragment, args);
        } else {
            Navigation.findNavController(requireView()).navigate(R.id.action_scheduledEmailsFragment_to_emailDetailsFragment, args);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
        binding = null;
    }
}
