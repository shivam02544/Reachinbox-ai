package com.reachinbox.app.ui.settings;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.reachinbox.app.R;
import com.reachinbox.app.adapter.TemplateAdapter;
import com.reachinbox.app.data.model.Template;
import com.reachinbox.app.data.model.TemplateListResponse;
import com.reachinbox.app.data.repository.TemplateRepository;
import com.reachinbox.app.databinding.FragmentTemplatesBinding;
import com.reachinbox.app.utils.DialogHelper;
import com.reachinbox.app.utils.FeedbackUtils;

import java.util.ArrayList;
import java.util.List;

public class TemplatesFragment extends Fragment implements TemplateAdapter.OnTemplateActionListener {

    private FragmentTemplatesBinding binding;
    private TemplateRepository templateRepository;
    private TemplateAdapter adapter;
    private final List<Template> allTemplates = new ArrayList<>();

    private String currentSearch = null;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTemplatesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        templateRepository = new TemplateRepository(requireContext());
        setupRecyclerView();
        setupSwipeRefresh();
        setupSearch();
        setupListeners();

        loadTemplates(true);
    }

    private void setupRecyclerView() {
        adapter = new TemplateAdapter(this);
        binding.rvTemplates.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvTemplates.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> loadTemplates(true));
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
                    currentSearch = s.toString().trim().isEmpty() ? null : s.toString().trim();
                    loadTemplates(true);
                };
                searchHandler.postDelayed(searchRunnable, 500);
            }
        });
    }

    private void setupListeners() {
        binding.fabCreateTemplate.setOnClickListener(v -> navigateToCreate());
        binding.btnCreateTemplateEmpty.setOnClickListener(v -> navigateToCreate());
    }

    private void navigateToCreate() {
        Navigation.findNavController(requireView()).navigate(R.id.action_templatesFragment_to_createEditTemplateFragment);
    }

    private void loadTemplates(boolean reset) {
        if (binding == null) return;
        if (reset) {
            allTemplates.clear();
            binding.swipeRefresh.setRefreshing(true);
        }

        templateRepository.getTemplates(1, 100, currentSearch, "updatedAt", "desc", new TemplateRepository.TemplateCallback<TemplateListResponse>() {
            @Override
            public void onSuccess(TemplateListResponse result) {
                if (isAdded() && binding != null) {
                    binding.swipeRefresh.setRefreshing(false);
                    allTemplates.clear();
                    allTemplates.addAll(result.getTemplates());
                    adapter.setTemplates(allTemplates);
                    updateUI();
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded() && binding != null) {
                    binding.swipeRefresh.setRefreshing(false);
                    FeedbackUtils.showError(requireView(), message);
                }
            }
        });
    }

    private void updateUI() {
        if (allTemplates.isEmpty()) {
            binding.rvTemplates.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.VISIBLE);
        } else {
            binding.rvTemplates.setVisibility(View.VISIBLE);
            binding.emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onEdit(Template template) {
        Bundle args = new Bundle();
        args.putString("templateId", template.getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_templatesFragment_to_createEditTemplateFragment, args);
    }

    @Override
    public void onDelete(Template template) {
        DialogHelper.showDestructiveConfirmation(
                requireContext(),
                "Delete Template",
                "Are you sure you want to delete this template?",
                "Delete",
                () -> performDelete(template.getId())
        );
    }

    private void performDelete(String id) {
        binding.progressBar.setVisibility(View.VISIBLE);
        templateRepository.deleteTemplate(id, new TemplateRepository.TemplateCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (isAdded()) {
                    binding.progressBar.setVisibility(View.GONE);
                    FeedbackUtils.showSuccess(requireView(), "Template deleted");
                    loadTemplates(true);
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    binding.progressBar.setVisibility(View.GONE);
                    FeedbackUtils.showError(requireView(), message);
                }
            }
        });
    }

    @Override
    public void onClick(Template template) {
        // Optional: Show details or just edit
        onEdit(template);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        binding = null;
    }
}
