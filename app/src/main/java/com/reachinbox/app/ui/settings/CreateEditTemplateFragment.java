package com.reachinbox.app.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.reachinbox.app.R;
import com.reachinbox.app.data.model.CreateTemplateRequest;
import com.reachinbox.app.data.model.Template;
import com.reachinbox.app.data.model.UpdateTemplateRequest;
import com.reachinbox.app.data.repository.TemplateRepository;
import com.reachinbox.app.databinding.FragmentCreateEditTemplateBinding;
import com.reachinbox.app.utils.FeedbackUtils;

public class CreateEditTemplateFragment extends Fragment {

    private FragmentCreateEditTemplateBinding binding;
    private TemplateRepository templateRepository;
    private String templateId; // If null, we are in Create mode

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateEditTemplateBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        templateRepository = new TemplateRepository(requireContext());

        if (getArguments() != null) {
            templateId = getArguments().getString("templateId");
        }

        setupUI();
        setupListeners();

        if (templateId != null) {
            loadTemplate();
        }
    }

    private void setupUI() {
        if (templateId != null) {
            binding.toolbar.setTitle("Edit Template");
            binding.btnSave.setText("Update Template");
        } else {
            binding.toolbar.setTitle("Create Template");
            binding.btnSave.setText("Save Template");
        }
    }

    private void setupListeners() {
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.btnSave.setOnClickListener(v -> handleSave());
    }

    private void loadTemplate() {
        setLoading(true);
        templateRepository.getTemplate(templateId, new TemplateRepository.TemplateCallback<Template>() {
            @Override
            public void onSuccess(Template result) {
                if (isAdded()) {
                    setLoading(false);
                    binding.etTemplateName.setText(result.getName());
                    binding.etSubject.setText(result.getSubject());
                    binding.etBody.setText(result.getBodyText());
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

    private void handleSave() {
        String name = binding.etTemplateName.getText().toString().trim();
        String subject = binding.etSubject.getText().toString().trim();
        String body = binding.etBody.getText().toString().trim();

        if (name.isEmpty() || subject.isEmpty() || body.isEmpty()) {
            FeedbackUtils.showError(binding.getRoot(), "Please fill all fields");
            return;
        }

        setLoading(true);

        if (templateId == null) {
            // Create mode
            CreateTemplateRequest request = new CreateTemplateRequest(name, subject, body);
            templateRepository.createTemplate(request, new TemplateRepository.TemplateCallback<Template>() {
                @Override
                public void onSuccess(Template result) {
                    if (isAdded()) {
                        setLoading(false);
                        FeedbackUtils.showSuccess(binding.getRoot(), "Template created");
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
        } else {
            // Edit mode
            UpdateTemplateRequest request = new UpdateTemplateRequest();
            request.setName(name);
            request.setSubject(subject);
            request.setBodyText(body);

            templateRepository.updateTemplate(templateId, request, new TemplateRepository.TemplateCallback<Template>() {
                @Override
                public void onSuccess(Template result) {
                    if (isAdded()) {
                        setLoading(false);
                        FeedbackUtils.showSuccess(binding.getRoot(), "Template updated");
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
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!isLoading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
