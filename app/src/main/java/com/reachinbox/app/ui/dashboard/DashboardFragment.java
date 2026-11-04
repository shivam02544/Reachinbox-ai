package com.reachinbox.app.ui.dashboard;

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
import com.reachinbox.app.data.local.SessionManager;
import com.reachinbox.app.data.model.DashboardData;
import com.reachinbox.app.data.model.RecentActivity;
import com.reachinbox.app.data.model.User;
import com.reachinbox.app.data.repository.DashboardRepository;
import com.reachinbox.app.adapter.ActivityAdapter;
import com.reachinbox.app.databinding.FragmentDashboardBinding;

import java.util.Calendar;
import java.util.ArrayList;

public class DashboardFragment extends Fragment implements ActivityAdapter.OnActivityClickListener {

    private FragmentDashboardBinding binding;
    private DashboardRepository dashboardRepository;
    private SessionManager sessionManager;
    private ActivityAdapter activityAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dashboardRepository = new DashboardRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        setupHeader();
        setupRecyclerView();
        setupSwipeRefresh();

        loadDashboardData();

        binding.btnRetry.setOnClickListener(v -> loadDashboardData());
        binding.btnCompose.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.composeEmailFragment)
        );

        binding.btnViewAllActivity.setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.activityFragment)
        );
    }

    private void setupHeader() {
        User user = sessionManager.getUser();
        if (user != null) {
            String greeting = getGreeting();
            binding.tvGreeting.setText(getString(R.string.greeting_format, greeting, user.getName()));
        }
    }

    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return getString(R.string.greeting_morning);
        if (hour < 17) return getString(R.string.greeting_afternoon);
        return getString(R.string.greeting_evening);
    }

    private void setupRecyclerView() {
        activityAdapter = new ActivityAdapter(this);
        binding.rvActivity.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvActivity.setAdapter(activityAdapter);
    }

    @Override
    public void onActivityClick(RecentActivity activity) {
        if (activity.getEmailId() != null) {
            Bundle args = new Bundle();
            args.putString("emailId", activity.getEmailId());
            Navigation.findNavController(requireView()).navigate(R.id.action_dashboardFragment_to_emailDetailsFragment, args);
        }
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(this::loadDashboardData);
    }

    private void loadDashboardData() {
        binding.swipeRefresh.setRefreshing(true);
        binding.errorState.setVisibility(View.GONE);

        dashboardRepository.getDashboard(new DashboardRepository.DashboardCallback() {
            @Override
            public void onSuccess(DashboardData data) {
                if (isAdded() && binding != null) {
                    binding.swipeRefresh.setRefreshing(false);
                    updateUI(data);
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded() && binding != null) {
                    binding.swipeRefresh.setRefreshing(false);
                    showError();
                }
            }
        });
    }

    private void updateUI(DashboardData data) {
        if (binding == null || data == null || data.getStats() == null) return;
        // Update Stats
        binding.tvTotalEmails.setText(String.valueOf(data.getStats().getTotalEmails()));
        binding.tvScheduled.setText(String.valueOf(data.getStats().getScheduled()));
        binding.tvSent.setText(String.valueOf(data.getStats().getSent()));
        binding.tvFailed.setText(String.valueOf(data.getStats().getFailed()));

        // Update Activity List
        if (data.getRecentActivity() != null && !data.getRecentActivity().isEmpty()) {
            activityAdapter.setActivities(new ArrayList<>(data.getRecentActivity().subList(
                    0, Math.min(3, data.getRecentActivity().size()))));
            binding.rvActivity.setVisibility(View.VISIBLE);
            binding.emptyState.setVisibility(View.GONE);
        } else {
            binding.rvActivity.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.VISIBLE);
        }
    }

    private void showError() {
        binding.errorState.setVisibility(View.VISIBLE);
        binding.rvActivity.setVisibility(View.GONE);
        binding.emptyState.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
