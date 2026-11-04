package com.reachinbox.app.ui.auth;

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
import com.reachinbox.app.data.local.SessionManager;
import com.reachinbox.app.data.model.AuthData;
import com.reachinbox.app.data.repository.AuthRepository;
import com.reachinbox.app.databinding.FragmentRegisterBinding;
import com.reachinbox.app.utils.FeedbackUtils;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthRepository authRepository;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepository = new AuthRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        binding.btnRegister.setOnClickListener(v -> handleRegister());
        binding.btnLogin.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
    }

    private void handleRegister() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        String confirmPassword = binding.etConfirmPassword.getText().toString();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            FeedbackUtils.showError(requireView(), getString(R.string.error_empty_fields));
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            FeedbackUtils.showError(requireView(), getString(R.string.error_invalid_email));
            return;
        }

        if (password.length() < 8) {
            FeedbackUtils.showError(requireView(), getString(R.string.error_password_length));
            return;
        }

        if (!password.equals(confirmPassword)) {
            FeedbackUtils.showError(requireView(), getString(R.string.error_password_mismatch));
            return;
        }

        setLoading(true);

        authRepository.register(name, email, password, new AuthRepository.AuthCallback<AuthData>() {
            @Override
            public void onSuccess(AuthData result) {
                if (isAdded() && binding != null) {
                    setLoading(false);
                    if (result == null || result.getUser() == null
                            || result.getAccessToken() == null || result.getRefreshToken() == null) {
                        FeedbackUtils.showError(requireView(), getString(R.string.error_auth));
                        return;
                    }
                    sessionManager.saveSession(result.getAccessToken(), result.getRefreshToken(), result.getUser());
                    navigateToDashboard();
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded() && binding != null) {
                    setLoading(false);
                    FeedbackUtils.showError(requireView(), message);
                }
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.btnRegister.setEnabled(!isLoading);
        binding.btnRegister.setText(R.string.btn_signup);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void navigateToDashboard() {
        Navigation.findNavController(requireView()).navigate(R.id.action_registerFragment_to_dashboardFragment);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
