package com.reachinbox.app.utils;

import android.content.Context;
import android.content.DialogInterface;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.reachinbox.app.R;

public class DialogHelper {

    public interface DialogCallback {
        void onPositive();
        default void onNegative() {}
    }

    public static void showConfirmation(Context context, String title, String message, String positiveButton, DialogCallback callback) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButton, (dialog, which) -> callback.onPositive())
                .setNegativeButton("Cancel", (dialog, which) -> callback.onNegative())
                .show();
    }

    public static void showDestructiveConfirmation(Context context, String title, String message, String destructiveButton, DialogCallback callback) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(destructiveButton, (dialog, which) -> callback.onPositive())
                .setNegativeButton("Cancel", (dialog, which) -> callback.onNegative())
                .show();
    }
}