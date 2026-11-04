package com.reachinbox.app.ui.activity;

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
import com.reachinbox.app.adapter.ActivityAdapter;
import com.reachinbox.app.data.model.ActivityListResponse;
import com.reachinbox.app.data.model.RecentActivity;
import com.reachinbox.app.data.repository.ActivityRepository;
import com.reachinbox.app.databinding.FragmentActivityBinding;
import com.reachinbox.app.utils.FeedbackUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

public class ActivityFragment extends Fragment implements ActivityAdapter.OnActivityClickListener {

    private FragmentActivityBinding binding;
    private ActivityRepository activityRepository;
    private ActivityAdapter adapter;
    private List<RecentActivity> allActivities = new ArrayList<>();
    private String currentFilter = "ALL";
    private boolean isLoading;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentActivityBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activityRepository = new ActivityRepository(requireContext());
        setupRecyclerView();
        setupSwipeRefresh();
        setupFilters();

        loadActivities();
    }

    private void setupFilters() {
        binding.chipGroupActivityFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checked = checkedIds.get(0);
            if (checked == R.id.chipActivityEmails) currentFilter = "EMAILS";
            else if (checked == R.id.chipActivityCampaigns) currentFilter = "CAMPAIGNS";
            else if (checked == R.id.chipActivitySenders) currentFilter = "SENDERS";
            else currentFilter = "ALL";
            renderActivities();
        });
    }

    private void setupRecyclerView() {
        adapter = new ActivityAdapter(this);
        binding.rvActivity.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvActivity.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(this::loadActivities);
    }

    private void loadActivities() {
        if (binding == null || isLoading) return;
        isLoading = true;
        binding.swipeRefresh.setRefreshing(true);
        activityRepository.getActivities(1, 100, new ActivityRepository.ActivityCallback() {
            @Override
            public void onSuccess(ActivityListResponse result) {
                if (isAdded() && binding != null) {
                    isLoading = false;
                    binding.swipeRefresh.setRefreshing(false);
                    allActivities = result == null || result.getActivities() == null
                            ? new ArrayList<>() : new ArrayList<>(result.getActivities());
                    renderActivities();
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded() && binding != null) {
                    isLoading = false;
                    binding.swipeRefresh.setRefreshing(false);
                    FeedbackUtils.showError(binding.getRoot(), message);
                }
            }
        });
    }

    private void renderActivities() {
        if (binding == null) return;
        List<RecentActivity> activities = new ArrayList<>();
        for (RecentActivity activity : allActivities) {
            String type = activity.getType() == null ? "" : activity.getType().toUpperCase(Locale.ROOT);
            boolean matches = "ALL".equals(currentFilter)
                    || ("EMAILS".equals(currentFilter) && type.startsWith("EMAIL_"))
                    || ("CAMPAIGNS".equals(currentFilter) && type.startsWith("CAMPAIGN_"))
                    || ("SENDERS".equals(currentFilter) && type.startsWith("SENDER_"));
            if (matches) activities.add(activity);
        }
        binding.tvActivityCount.setText("ALL".equals(currentFilter)
                ? getString(R.string.activity_event_count, allActivities.size())
                : getString(R.string.activity_event_count_filtered, activities.size(), allActivities.size()));
        if (activities.isEmpty()) {
            binding.rvActivity.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.tvEmptyTitle.setText("ALL".equals(currentFilter)
                    ? R.string.msg_no_activity : R.string.msg_no_filtered_activity);
        } else {
            adapter.setActivities(activities);
            binding.rvActivity.setVisibility(View.VISIBLE);
            binding.emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityClick(RecentActivity activity) {
        if (activity.getEmailId() != null) {
            Bundle args = new Bundle();
            args.putString("emailId", activity.getEmailId());
            Navigation.findNavController(requireView()).navigate(R.id.action_activityFragment_to_emailDetailsFragment, args);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isLoading = false;
        binding = null;
    }
}
