package com.reachinbox.app.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.reachinbox.app.R;
import com.reachinbox.app.data.model.ScheduledEmail;
import com.reachinbox.app.databinding.ItemScheduledEmailBinding;

import java.util.ArrayList;
import java.util.List;

public class ScheduledEmailAdapter extends RecyclerView.Adapter<ScheduledEmailAdapter.ViewHolder> {

    private List<ScheduledEmail> emails = new ArrayList<>();
    private final OnActionListener listener;

    public interface OnActionListener {
        void onCancel(ScheduledEmail email);
        void onEmailClick(ScheduledEmail email);
    }

    public ScheduledEmailAdapter(OnActionListener listener) {
        this.listener = listener;
    }

    public void setEmails(List<ScheduledEmail> emails) {
        this.emails = emails;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemScheduledEmailBinding binding = ItemScheduledEmailBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduledEmail scheduled = emails.get(position);
        holder.binding.tvRecipient.setText(scheduled.getEmail().getRecipient());
        holder.binding.tvSubject.setText(scheduled.getEmail().getSubject());
        holder.binding.tvStatus.setText(scheduled.getStatus());

        if (scheduled.getEmail().getSenderAccount() != null) {
            holder.binding.tvSender.setText("From: " + scheduled.getEmail().getSenderAccount().getEmail());
        }

        CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                scheduled.getScheduledAt().getTime(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS);
        holder.binding.tvTime.setText("Scheduled for: " + relativeTime);

        // Status styling
        int backgroundRes = R.drawable.bg_badge_neutral;
        int textColorRes = R.color.colorOnNeutralContainer;
        
        if (scheduled.getStatus() != null) {
            switch (scheduled.getStatus()) {
                case "SENT":
                    backgroundRes = R.drawable.bg_badge_success;
                    textColorRes = R.color.colorOnSuccessContainer;
                    break;
                case "FAILED":
                    backgroundRes = R.drawable.bg_badge_error;
                    textColorRes = R.color.colorOnErrorContainer;
                    break;
                case "PROCESSING":
                    backgroundRes = R.drawable.bg_badge_info;
                    textColorRes = R.color.colorOnInfoContainer;
                    break;
                case "SCHEDULED":
                    backgroundRes = R.drawable.bg_badge_info;
                    textColorRes = R.color.colorOnInfoContainer;
                    break;
                case "CANCELLED":
                    backgroundRes = R.drawable.bg_badge_neutral;
                    textColorRes = R.color.colorOnNeutralContainer;
                    break;
            }
        }
        holder.binding.tvStatus.setBackgroundResource(backgroundRes);
        holder.binding.tvStatus.setTextColor(holder.itemView.getContext().getColor(textColorRes));

        // Show cancel button only if status is SCHEDULED
        if ("SCHEDULED".equals(scheduled.getStatus())) {
            holder.binding.btnCancel.setVisibility(android.view.View.VISIBLE);
            holder.binding.btnCancel.setOnClickListener(v -> listener.onCancel(scheduled));
        } else {
            holder.binding.btnCancel.setVisibility(android.view.View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onEmailClick(scheduled));
    }

    @Override
    public int getItemCount() {
        return emails.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemScheduledEmailBinding binding;

        public ViewHolder(ItemScheduledEmailBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
