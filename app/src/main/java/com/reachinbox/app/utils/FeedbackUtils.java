package com.reachinbox.app.utils;

import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.reachinbox.app.R;

public class FeedbackUtils {

    public static void showSuccess(View view, String message) {
        showSnackbar(view, message, R.color.colorSuccess, R.color.colorOnSuccess, R.drawable.ic_launcher_foreground);
    }

    public static void showError(View view, String message) {
        showSnackbar(view, message, R.color.colorError, R.color.colorOnError, R.drawable.ic_warning);
    }

    public static void showInfo(View view, String message) {
        showSnackbar(view, message, R.color.colorInfo, R.color.colorOnInfo, R.drawable.ic_activity);
    }

    private static void showSnackbar(View view, String message, int bgColorRes, int textColorRes, int iconRes) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(ContextCompat.getColor(view.getContext(), bgColorRes));
        snackbar.setTextColor(ContextCompat.getColor(view.getContext(), textColorRes));
        
        View snackbarView = snackbar.getView();
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
        textView.setCompoundDrawablePadding(view.getContext().getResources().getDimensionPixelOffset(R.dimen.spacing_12));
        
        snackbar.show();
    }
}