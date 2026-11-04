package com.reachinbox.app.ui.profile;

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
import androidx.navigation.Navigation;

import com.reachinbox.app.R;
import com.reachinbox.app.data.local.SessionManager;
import com.reachinbox.app.data.model.User;
import com.reachinbox.app.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        User user = new SessionManager(requireContext()).getUser();
        String name = user == null || user.getName() == null || user.getName().trim().isEmpty()
                ? getString(R.string.default_user_name) : user.getName().trim();
        String email = user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()
                ? getString(R.string.default_user_email) : user.getEmail().trim();

        binding.tvProfileName.setText(name);
        binding.tvProfileNameValue.setText(name);
        binding.tvProfileEmail.setText(email);
        binding.tvProfileEmailValue.setText(email);
        binding.tvProfileInitials.setText(initials(name));
        loadAvatar(user == null ? null : user.getAvatarUrl());
        binding.btnProfileSettings.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.settingsFragment));
    }

    private void loadAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) return;
        Bitmap bitmap = null;
        if (avatarUrl.startsWith("data:image/")) {
            int separator = avatarUrl.indexOf(',');
            if (separator > 0) {
                try {
                    byte[] image = Base64.decode(avatarUrl.substring(separator + 1), Base64.DEFAULT);
                    bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
                } catch (IllegalArgumentException ignored) {
                    // Keep initials if inline image data is invalid.
                }
            }
        } else if (avatarUrl.startsWith("content://")) {
            try (InputStream input = requireContext().getContentResolver().openInputStream(Uri.parse(avatarUrl))) {
                if (input != null) bitmap = BitmapFactory.decodeStream(input);
            } catch (Exception ignored) {
                // Keep initials if a local avatar URI is unavailable.
            }
        }
        if (bitmap != null) {
            binding.ivProfileAvatar.setImageBitmap(bitmap);
            binding.ivProfileAvatar.setVisibility(View.VISIBLE);
            binding.tvProfileInitials.setVisibility(View.GONE);
        }
    }

    private String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return "R";
        StringBuilder result = new StringBuilder(parts[0].substring(0, 1));
        if (parts.length > 1 && !parts[parts.length - 1].isEmpty()) {
            result.append(parts[parts.length - 1].substring(0, 1));
        }
        return result.toString().toUpperCase(java.util.Locale.ROOT);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
