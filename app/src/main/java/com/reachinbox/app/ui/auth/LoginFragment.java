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
import com.reachinbox.app.databinding.FragmentLoginBinding;
import com.reachinbox.app.utils.FeedbackUtils;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthRepository authRepository;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepository = new AuthRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        // Initial check for session validation
        if (sessionManager.isLoggedIn()) {
            validateSession();
        }

        binding.btnLogin.setOnClickListener(v -> handleLogin());
        binding.btnRegister.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_registerFragment)
        );
    }

    private void validateSession() {
        setLoading(true);
        authRepository.getSession(new AuthRepository.AuthCallback<AuthData>() {
            @Override
            public void onSuccess(AuthData result) {
                if (isAdded()) {
                    setLoading(false);
                    // Refresh successful, update user data if needed and go to dashboard
                    navigateToDashboard();
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    setLoading(false);
                    // Session invalid or expired, clear it only if it's an authentication error
                    if ("Invalid email or password".equals(message)) {
                        sessionManager.clearSession();
                        FeedbackUtils.showError(requireView(), getString(R.string.error_session_expired));
                    } else {
                        // Network error or server down, just stay on login but don't clear local data
                        FeedbackUtils.showError(requireView(), message);
                    }
                }
            }
        });
    }

    private void handleLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            FeedbackUtils.showError(requireView(), getString(R.string.error_empty_fields));
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            FeedbackUtils.showError(requireView(), getString(R.string.error_invalid_email));
            return;
        }

        setLoading(true);

        authRepository.login(email, password, new AuthRepository.AuthCallback<AuthData>() {
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
        binding.btnLogin.setEnabled(!isLoading);
        binding.btnLogin.setText(R.string.btn_login);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void navigateToDashboard() {
        Navigation.findNavController(requireView()).navigate(R.id.action_loginFragment_to_dashboardFragment);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
