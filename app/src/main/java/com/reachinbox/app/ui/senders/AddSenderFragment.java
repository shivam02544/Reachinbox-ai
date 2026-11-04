package com.reachinbox.app.ui.senders;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.reachinbox.app.R;
import com.reachinbox.app.data.model.CreateSenderRequest;
import com.reachinbox.app.data.model.Sender;
import com.reachinbox.app.data.repository.SenderRepository;
import com.reachinbox.app.databinding.FragmentAddSenderBinding;
import com.reachinbox.app.utils.FeedbackUtils;

public class AddSenderFragment extends Fragment {

    private FragmentAddSenderBinding binding;
    private SenderRepository senderRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddSenderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        senderRepository = new SenderRepository(requireContext());

        binding.btnVerifyAdd.setOnClickListener(v -> handleAddSender());
    }

    private void handleAddSender() {
        String displayName = binding.etDisplayName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String host = binding.etSmtpHost.getText().toString().trim();
        String portStr = binding.etSmtpPort.getText().toString().trim();
        String username = binding.etSmtpUsername.getText().toString().trim();
        String password = binding.etSmtpPassword.getText().toString().trim();

        if (displayName.isEmpty() || email.isEmpty() || host.isEmpty() || portStr.isEmpty() || username.isEmpty() || password.isEmpty()) {
            FeedbackUtils.showError(requireView(), getString(R.string.error_empty_fields));
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            FeedbackUtils.showError(requireView(), getString(R.string.error_invalid_email));
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            FeedbackUtils.showError(requireView(), "Invalid port");
            return;
        }

        setLoading(true);

        CreateSenderRequest request = new CreateSenderRequest(displayName, email, host, port, username, password);

        senderRepository.createSender(request, new SenderRepository.SenderCallback<>() {
            @Override
            public void onSuccess(Sender sender) {
                // Now verify the sender
                verifyCreatedSender(sender.getId());
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    setLoading(false);
                    FeedbackUtils.showError(requireView(), message);
                }
            }
        });
    }

    private void verifyCreatedSender(String senderId) {
        senderRepository.verifySender(senderId, new SenderRepository.SenderCallback<>() {
            @Override
            public void onSuccess(Sender result) {
                if (isAdded()) {
                    setLoading(false);
                    if ("CONNECTED".equals(result.getStatus())) {
                        FeedbackUtils.showSuccess(requireView(), getString(R.string.msg_sender_added));
                        Navigation.findNavController(requireView()).navigateUp();
                    } else {
                        FeedbackUtils.showError(requireView(), getString(R.string.msg_sender_verify_fail));
                    }
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    setLoading(false);
                    FeedbackUtils.showError(requireView(), message);
                }
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.btnVerifyAdd.setEnabled(!isLoading);
        binding.btnVerifyAdd.setText(isLoading ? "" : getString(R.string.btn_verify_add));
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
