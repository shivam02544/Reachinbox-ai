package com.reachinbox.app.ui.campaign;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.reachinbox.app.R;
import com.reachinbox.app.adapter.CampaignAdapter;
import com.reachinbox.app.data.model.Campaign;
import com.reachinbox.app.data.model.CampaignListResponse;
import com.reachinbox.app.data.repository.CampaignRepository;
import com.reachinbox.app.databinding.FragmentCampaignsBinding;
import com.reachinbox.app.utils.FeedbackUtils;

import java.util.ArrayList;
import java.util.List;

public class CampaignsFragment extends Fragment implements CampaignAdapter.OnCampaignClickListener {

    private FragmentCampaignsBinding binding;
    private CampaignRepository campaignRepository;
    private CampaignAdapter adapter;
    
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasNextPage = true;
    private List<Campaign> allCampaigns = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCampaignsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        campaignRepository = new CampaignRepository(requireContext());
        setupRecyclerView();
        setupSwipeRefresh();

        binding.fabCreateCampaign.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_campaignsFragment_to_createCampaignFragment)
        );
        binding.btnCreateCampaignEmpty.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_campaignsFragment_to_createCampaignFragment)
        );
        binding.btnRetry.setOnClickListener(v -> loadCampaigns(true));

        loadCampaigns(true);
    }

    private void setupRecyclerView() {
        adapter = new CampaignAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.rvCampaigns.setLayoutManager(layoutManager);
        binding.rvCampaigns.setAdapter(adapter);

        binding.rvCampaigns.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && hasNextPage) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                            loadCampaigns(false);
                        }
                    }
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> loadCampaigns(true));
    }

    private void loadCampaigns(boolean reset) {
        if (binding == null) return;
        if (reset) {
            currentPage = 1;
            hasNextPage = true;
            allCampaigns.clear();
            binding.swipeRefresh.setRefreshing(true);
        }

        isLoading = true;
        binding.errorState.setVisibility(View.GONE);

        campaignRepository.getCampaigns(currentPage, 20, new CampaignRepository.CampaignCallback<CampaignListResponse>() {
            @Override
            public void onSuccess(CampaignListResponse result) {
                if (isAdded() && binding != null) {
                    isLoading = false;
                    binding.swipeRefresh.setRefreshing(false);
                    binding.progressBar.setVisibility(View.GONE);

                    List<Campaign> newCampaigns = result.getCampaigns();
                    allCampaigns.addAll(newCampaigns);
                    adapter.setCampaigns(allCampaigns);

                    hasNextPage = result.getPagination().getPage() < result.getPagination().getTotalPages();
                    if (hasNextPage) {
                        currentPage++;
                    }

                    updateUI();
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded() && binding != null) {
                    isLoading = false;
                    binding.swipeRefresh.setRefreshing(false);
                    binding.progressBar.setVisibility(View.GONE);
                    
                    if (allCampaigns.isEmpty()) {
                        binding.errorState.setVisibility(View.VISIBLE);
                        binding.rvCampaigns.setVisibility(View.GONE);
                    } else {
                        FeedbackUtils.showError(requireView(), message);
                    }
                }
            }
        });
    }

    private void updateUI() {
        if (binding == null) return;
        if (allCampaigns.isEmpty()) {
            binding.rvCampaigns.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.VISIBLE);
        } else {
            binding.rvCampaigns.setVisibility(View.VISIBLE);
            binding.emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCampaignClick(Campaign campaign) {
        Bundle args = new Bundle();
        args.putString("campaignId", campaign.getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_campaignsFragment_to_campaignDetailsFragment, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
