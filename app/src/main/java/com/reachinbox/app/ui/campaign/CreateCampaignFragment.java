package com.reachinbox.app.ui.campaign;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.reachinbox.app.R;
import com.reachinbox.app.data.model.AddRecipientsRequest;
import com.reachinbox.app.data.model.Campaign;
import com.reachinbox.app.data.model.CreateCampaignRequest;
import com.reachinbox.app.data.model.Sender;
import com.reachinbox.app.data.model.Template;
import com.reachinbox.app.data.model.TemplateListResponse;
import com.reachinbox.app.data.repository.CampaignRepository;
import com.reachinbox.app.data.repository.SenderRepository;
import com.reachinbox.app.data.repository.TemplateRepository;
import com.reachinbox.app.databinding.FragmentCreateCampaignBinding;
import com.reachinbox.app.utils.FeedbackUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CreateCampaignFragment extends Fragment {

    private FragmentCreateCampaignBinding binding;
    private CampaignRepository campaignRepository;
    private SenderRepository senderRepository;
    private TemplateRepository templateRepository;
    
    private List<Sender> connectedSenders = new ArrayList<>();
    private Sender selectedSender;
    private int currentStep = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateCampaignBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        campaignRepository = new CampaignRepository(requireContext());
        senderRepository = new SenderRepository(requireContext());
        templateRepository = new TemplateRepository(requireContext());

        setupToolbar();
        loadSenders();
        setupListeners();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());
    }

    private void loadSenders() {
        senderRepository.getSenders(new SenderRepository.SenderCallback<List<Sender>>() {
            @Override
            public void onSuccess(List<Sender> result) {
                if (isAdded()) {
                    connectedSenders = new ArrayList<>();
                    for (Sender s : result) {
                        if ("CONNECTED".equals(s.getStatus())) {
                            connectedSenders.add(s);
                        }
                    }

                    if (connectedSenders.isEmpty()) {
                        binding.actvSender.setEnabled(false);
                        binding.noSenderContainer.setVisibility(View.VISIBLE);
                    } else {
                        binding.actvSender.setEnabled(true);
                        binding.noSenderContainer.setVisibility(View.GONE);

                        List<String> names = new ArrayList<>();
                        for (Sender s : connectedSenders) {
                            names.add(s.getDisplayName() + " (" + s.getEmail() + ")");
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                                android.R.layout.simple_dropdown_item_1line, names);
                        binding.actvSender.setAdapter(adapter);
                        binding.actvSender.setOnItemClickListener((parent, view1, position, id) -> {
                            selectedSender = connectedSenders.get(position);
                        });
                    }
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    FeedbackUtils.showError(binding.getRoot(), message);
                }
            }
        });
    }

    private void setupListeners() {
        binding.btnNext.setOnClickListener(v -> handleNext());
        binding.btnBack.setOnClickListener(v -> handleBack());
        binding.btnAddSender.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.sendersFragment)
        );
        
        binding.btnUseTemplate.setOnClickListener(v -> showTemplatePicker());

        binding.etRecipients.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                List<String> recipients = parseRecipients(s.toString());
                binding.tvRecipientCount.setText(recipients.size() + " recipients");
            }
        });
    }

    private void showTemplatePicker() {
        templateRepository.getTemplates(1, 100, null, "updatedAt", "desc", new TemplateRepository.TemplateCallback<TemplateListResponse>() {
            @Override
            public void onSuccess(TemplateListResponse result) {
                if (isAdded()) {
                    List<Template> templates = result.getTemplates();
                    if (templates.isEmpty()) {
                        FeedbackUtils.showInfo(binding.getRoot(), "No templates found. Create one in More > Templates.");
                        return;
                    }

                    String[] names = new String[templates.size()];
                    for (int i = 0; i < templates.size(); i++) {
                        names[i] = templates.get(i).getName();
                    }

                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Select Template")
                            .setItems(names, (dialog, which) -> applyTemplate(templates.get(which)))
                            .show();
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    FeedbackUtils.showError(binding.getRoot(), "Failed to load templates: " + message);
                }
            }
        });
    }

    private void applyTemplate(Template template) {
        String currentSubject = binding.etSubject.getText().toString().trim();
        String currentBody = binding.etBody.getText().toString().trim();

        if (!currentSubject.isEmpty() || !currentBody.isEmpty()) {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Replace content?")
                    .setMessage("This will replace your current subject and message. Continue?")
                    .setPositiveButton("Replace", (dialog, which) -> {
                        binding.etSubject.setText(template.getSubject());
                        binding.etBody.setText(template.getBodyText());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            binding.etSubject.setText(template.getSubject());
            binding.etBody.setText(template.getBodyText());
        }
    }

    private void handleNext() {
        if (currentStep == 1) {
            if (validateStep1()) {
                currentStep = 2;
                updateStepUI();
            }
        } else if (currentStep == 2) {
            if (validateStep2()) {
                currentStep = 3;
                updateStepUI();
            }
        } else if (currentStep == 3) {
            if (validateStep3()) {
                createCampaign();
            }
        }
    }

    private void handleBack() {
        if (currentStep > 1) {
            currentStep--;
            updateStepUI();
        }
    }

    private void updateStepUI() {
        binding.viewFlipper.setDisplayedChild(currentStep - 1);
        binding.creationProgress.setProgress(currentStep);
        binding.btnBack.setVisibility(currentStep > 1 ? View.VISIBLE : View.GONE);
        binding.btnNext.setText(currentStep == 3 ? getString(R.string.btn_create_finalize) : "Next");
    }

    private boolean validateStep1() {
        String name = binding.etCampaignName.getText().toString().trim();
        String subject = binding.etSubject.getText().toString().trim();
        String body = binding.etBody.getText().toString().trim();

        if (name.isEmpty() || subject.isEmpty() || body.isEmpty()) {
            FeedbackUtils.showError(binding.getRoot(), "Please fill all fields");
            return false;
        }
        return true;
    }

    private boolean validateStep2() {
        if (selectedSender == null) {
            FeedbackUtils.showError(binding.getRoot(), "Please select a sender");
            return false;
        }
        return true;
    }

    private boolean validateStep3() {
        List<String> recipients = parseRecipients(binding.etRecipients.getText().toString());
        if (recipients.isEmpty()) {
            FeedbackUtils.showError(binding.getRoot(), "Please add at least one recipient");
            return false;
        }
        return true;
    }

    private List<String> parseRecipients(String input) {
        String[] parts = input.split("[,\\n\\s]+");
        Set<String> unique = new HashSet<>();
        for (String p : parts) {
            String email = p.trim();
            if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                unique.add(email);
            }
        }
        return new ArrayList<>(unique);
    }

    private void createCampaign() {
        setLoading(true);
        String name = binding.etCampaignName.getText().toString().trim();
        String subject = binding.etSubject.getText().toString().trim();
        String body = binding.etBody.getText().toString().trim();

        CreateCampaignRequest request = new CreateCampaignRequest(name, selectedSender.getId(), subject, body);
        
        campaignRepository.createCampaign(request, new CampaignRepository.CampaignCallback<Campaign>() {
            @Override
            public void onSuccess(Campaign result) {
                addRecipients(result.getId());
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

    private void addRecipients(String campaignId) {
        List<String> recipients = parseRecipients(binding.etRecipients.getText().toString());
        AddRecipientsRequest request = new AddRecipientsRequest(recipients);

        campaignRepository.addRecipients(campaignId, request, new CampaignRepository.CampaignCallback<Object>() {
            @Override
            public void onSuccess(Object result) {
                finalizeCampaign(campaignId);
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    setLoading(false);
                    FeedbackUtils.showError(binding.getRoot(), "Campaign created, but failed to add recipients: " + message);
                    // Navigate to details anyway
                    Bundle args = new Bundle();
                    args.putString("campaignId", campaignId);
                    Navigation.findNavController(requireView()).navigate(R.id.action_createCampaignFragment_to_campaignDetailsFragment, args);
                }
            }
        });
    }

    private void finalizeCampaign(String campaignId) {
        campaignRepository.markReady(campaignId, new CampaignRepository.CampaignCallback<Campaign>() {
            @Override
            public void onSuccess(Campaign result) {
                if (isAdded()) {
                    setLoading(false);
                    FeedbackUtils.showSuccess(binding.getRoot(), "Campaign ready!");
                    Bundle args = new Bundle();
                    args.putString("campaignId", campaignId);
                    Navigation.findNavController(requireView()).navigate(R.id.action_createCampaignFragment_to_campaignDetailsFragment, args);
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    setLoading(false);
                    FeedbackUtils.showError(binding.getRoot(), "Campaign created, but failed to mark as ready: " + message);
                    Bundle args = new Bundle();
                    args.putString("campaignId", campaignId);
                    Navigation.findNavController(requireView()).navigate(R.id.action_createCampaignFragment_to_campaignDetailsFragment, args);
                }
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnNext.setEnabled(!isLoading);
        binding.btnBack.setEnabled(!isLoading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
