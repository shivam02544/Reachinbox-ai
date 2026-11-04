package com.reachinbox.app.ui.more;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.reachinbox.app.R;
import com.reachinbox.app.data.local.SessionManager;
import com.reachinbox.app.data.model.User;
import com.reachinbox.app.data.repository.AuthRepository;
import com.reachinbox.app.databinding.FragmentMoreBinding;
import com.reachinbox.app.utils.DialogHelper;

public class MoreFragment extends Fragment {

    private FragmentMoreBinding binding;
    private SessionManager sessionManager;
    private AuthRepository authRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMoreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        sessionManager = new SessionManager(requireContext());
        authRepository = new AuthRepository(requireContext());
        setupUserProfile();
        binding.profileHeader.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.profileFragment));

        binding.btnDrafts.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.draftsFragment)
        );

        binding.btnResume.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.resumeFragment)
        );

        binding.btnTemplates.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.templatesFragment)
        );

        binding.btnSenders.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.sendersFragment)
        );

        binding.btnSettings.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.settingsFragment)
        );

        binding.btnTheme.setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.settingsFragment)
        );

        binding.btnLogout.setOnClickListener(v -> handleLogout());
        
        binding.btnAbout.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle(R.string.label_about_reachinbox)
                .setMessage(R.string.about_description)
                .setPositiveButton(android.R.string.ok, null)
                .show());
    }

    private void setupUserProfile() {
        User user = sessionManager.getUser();
        if (user != null) {
            binding.txtUserName.setText(user.getName() == null || user.getName().trim().isEmpty()
                    ? getString(R.string.default_user_name) : user.getName());
            binding.txtUserEmail.setText(user.getEmail());
            binding.tvAvatar.setText(initials(user.getName()));
            loadAvatar(user.getAvatarUrl());
        } else {
            binding.txtUserName.setText(R.string.default_user_name);
            binding.txtUserEmail.setText(R.string.default_user_email);
            binding.tvAvatar.setText("R");
            binding.tvAvatar.setVisibility(View.VISIBLE);
            binding.ivAvatar.setVisibility(View.GONE);
        }
    }

    private String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "R";
        String[] parts = name.trim().split("\\s+");
        StringBuilder value = new StringBuilder(parts[0].substring(0, 1));
        if (parts.length > 1 && !parts[parts.length - 1].isEmpty()) {
            value.append(parts[parts.length - 1].substring(0, 1));
        }
        return value.toString().toUpperCase(java.util.Locale.ROOT);
    }

    private void loadAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            showInitialsAvatar();
            return;
        }
        if (avatarUrl.startsWith("data:image/")) {
            int separator = avatarUrl.indexOf(',');
            if (separator > 0) {
                try {
                    byte[] image = Base64.decode(avatarUrl.substring(separator + 1), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
                    if (bitmap != null) {
                        binding.ivAvatar.setImageBitmap(bitmap);
                        binding.ivAvatar.setVisibility(View.VISIBLE);
                        binding.tvAvatar.setVisibility(View.GONE);
                        return;
                    }
                } catch (IllegalArgumentException ignored) {
                    // Invalid inline image data falls back to initials.
                }
            }
            showInitialsAvatar();
            return;
        }
        if (avatarUrl.startsWith("content://")) {
            try (InputStream input = requireContext().getContentResolver().openInputStream(Uri.parse(avatarUrl))) {
                Bitmap bitmap = input == null ? null : BitmapFactory.decodeStream(input);
                if (bitmap != null) {
                    binding.ivAvatar.setImageBitmap(bitmap);
                    binding.ivAvatar.setVisibility(View.VISIBLE);
                    binding.tvAvatar.setVisibility(View.GONE);
                    return;
                }
            } catch (Exception ignored) {
                // Missing or revoked local avatar URI falls back to initials.
            }
            showInitialsAvatar();
            return;
        }
        showInitialsAvatar();
    }

    private void showInitialsAvatar() {
        binding.ivAvatar.setImageDrawable(null);
        binding.ivAvatar.setVisibility(View.GONE);
        binding.tvAvatar.setVisibility(View.VISIBLE);
    }

    private void handleLogout() {
        DialogHelper.showDestructiveConfirmation(
            requireContext(),
            "Logout?",
            "Are you sure you want to logout from ReachInbox?",
            "Logout",
            new DialogHelper.DialogCallback() {
                @Override
                public void onPositive() {
                    performLogout();
                }
            }
        );
    }

    private void performLogout() {
        String refreshToken = sessionManager.getRefreshToken();
        if (refreshToken != null) {
            authRepository.logout(refreshToken, new AuthRepository.AuthCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    performLocalLogout();
                }

                @Override
                public void onError(String message) {
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
