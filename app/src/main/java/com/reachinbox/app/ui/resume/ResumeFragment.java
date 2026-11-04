package com.reachinbox.app.ui.resume;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.reachinbox.app.R;
import com.reachinbox.app.data.model.Resume;
import com.reachinbox.app.data.model.ResumeStructuredData;
import com.reachinbox.app.data.repository.ResumeRepository;
import com.reachinbox.app.databinding.FragmentResumeBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ResumeFragment extends Fragment {

    private FragmentResumeBinding binding;
    private ResumeRepository repository;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        handleSelectedFile(uri);
                    }
                }
            }
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentResumeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new ResumeRepository(requireContext());

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        binding.btnUpload.setOnClickListener(v -> openFilePicker());
        binding.btnReplace.setOnClickListener(v -> openFilePicker());
        binding.btnDelete.setOnClickListener(v -> deleteResume());
        binding.btnNextJob.setOnClickListener(v -> navigateToJobRequirement());

        loadResume();
    }

    private void navigateToJobRequirement() {
        if (getView() != null) {
            Navigation.findNavController(getView()).navigate(R.id.action_resumeFragment_to_jobRequirementFragment);
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Select PDF Resume"));
    }

    private void handleSelectedFile(Uri uri) {
        try {
            showLoading("Extracting and analyzing resume...");
            File tempFile = createTempFileFromUri(uri);
            if (tempFile == null) {
                showError("Could not read the selected file.");
                return;
            }

            repository.uploadResume(tempFile, new ResumeRepository.ResumeCallback() {
                @Override
                public void onSuccess(Resume resume) {
                    tempFile.delete(); // Cleanup
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Resume uploaded and analyzed successfully", Toast.LENGTH_SHORT).show();
                    }
                    displayResume(resume);
                }

                @Override
                public void onError(String message) {
                    tempFile.delete(); // Cleanup
                    showError(message);
                    loadResume(); // Reload previous state
                }
            });

        } catch (Exception e) {
            showError("Error processing file: " + e.getMessage());
            loadResume();
        }
    }

    private File createTempFileFromUri(Uri uri) throws Exception {
        String fileName = getFileName(uri);
        File tempFile = new File(requireContext().getCacheDir(), fileName != null ? fileName : "temp_resume.pdf");
        
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(tempFile)) {
            if (is == null) return null;
            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            return tempFile;
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx != -1) {
                        result = cursor.getString(idx);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }

    private void loadResume() {
        showLoading("Loading resume...");
        repository.getResume(new ResumeRepository.ResumeCallback() {
            @Override
            public void onSuccess(Resume resume) {
                if (resume == null || resume.getStructuredData() == null) {
                    showEmptyState();
                } else {
                    displayResume(resume);
                }
            }

            @Override
            public void onError(String message) {
                showError(message);
                showEmptyState(); // Fallback
            }
        });
    }

    private void deleteResume() {
        showLoading("Deleting resume...");
        repository.deleteResume(new ResumeRepository.DeleteCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), "Resume deleted", Toast.LENGTH_SHORT).show();
                showEmptyState();
            }

            @Override
            public void onError(String message) {
                showError(message);
                loadResume(); // Revert UI
            }
        });
    }

    private void showLoading(String message) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvStatus.setVisibility(View.VISIBLE);
        binding.tvStatus.setText(message);
        binding.layoutEmpty.setVisibility(View.GONE);
        binding.layoutContent.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        binding.progressBar.setVisibility(View.GONE);
        binding.tvStatus.setVisibility(View.GONE);
        binding.layoutContent.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.VISIBLE);
    }

    private void displayResume(Resume resume) {
        binding.progressBar.setVisibility(View.GONE);
        binding.tvStatus.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.GONE);
        binding.layoutContent.setVisibility(View.VISIBLE);

        ResumeStructuredData data = resume.getStructuredData();
        if (data != null) {
            binding.tvResumeName.setText(data.getName() != null ? data.getName() : "Unknown");
            binding.tvResumeSummary.setText(data.getSummary() != null ? data.getSummary() : "No summary provided.");
            
            if (data.getSkills() != null && !data.getSkills().isEmpty()) {
                binding.tvResumeSkills.setText(String.join(", ", data.getSkills()));
            } else {
                binding.tvResumeSkills.setText("None extracted");
            }

            if (data.getExperience() != null && !data.getExperience().isEmpty()) {
                StringBuilder exp = new StringBuilder();
                for (String e : data.getExperience()) {
                    exp.append("• ").append(e).append("\n");
                }
                binding.tvResumeExperience.setText(exp.toString().trim());
            } else {
                binding.tvResumeExperience.setText("No experience extracted");
            }
        }
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
