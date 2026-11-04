package com.reachinbox.app.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.reachinbox.app.R;
import com.reachinbox.app.data.local.SessionManager;
import com.reachinbox.app.data.repository.AuthRepository;
import com.reachinbox.app.databinding.FragmentSettingsBinding;
import com.reachinbox.app.utils.DialogHelper;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SessionManager sessionManager;
    private AuthRepository authRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        authRepository = new AuthRepository(requireContext());

        setupThemeSelection();
        binding.btnProfile.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.profileFragment));
        binding.btnLogout.setOnClickListener(v -> handleLogout());
    }

    private void setupThemeSelection() {
        int mode = sessionManager.getThemeMode();
        switch (mode) {
            case SessionManager.THEME_LIGHT:
                binding.toggleTheme.check(R.id.btn_theme_light);
                break;
            case SessionManager.THEME_DARK:
                binding.toggleTheme.check(R.id.btn_theme_dark);
                break;
            default:
                binding.toggleTheme.check(R.id.btn_theme_system);
                break;
        }

        binding.toggleTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                int selectedMode;
                if (checkedId == R.id.btn_theme_light) {
                    selectedMode = SessionManager.THEME_LIGHT;
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                } else if (checkedId == R.id.btn_theme_dark) {
                    selectedMode = SessionManager.THEME_DARK;
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    selectedMode = SessionManager.THEME_SYSTEM;
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                }
                sessionManager.setThemeMode(selectedMode);
            }
        });
    }

    private void handleLogout() {
        DialogHelper.showConfirmation(
                requireContext(),
                "Logout",
                "Are you sure you want to logout?",
                "Logout",
                new DialogHelper.DialogCallback() {
                    @Override
                    public void onPositive() {
                        performLogoutApi();
                    }
                }
        );
    }

    private void performLogoutApi() {
        String refreshToken = sessionManager.getRefreshToken();
        if (refreshToken != null) {
            authRepository.logout(refreshToken, new AuthRepository.AuthCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    performLocalLogout();
                }

                @Override
                public void onError(String message) {
                    // Still logout locally if network fails
                    performLocalLogout();
                }
            });
        } else {
            performLocalLogout();
        }
    }

    private void performLocalLogout() {
        if (isAdded()) {
            sessionManager.clearSession();
            Navigation.findNavController(requireView()).navigate(
                    R.id.loginFragment,
                    null,
                    new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true)
                            .build()
            );
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
