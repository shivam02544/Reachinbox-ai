package com.reachinbox.app.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.reachinbox.app.R;
import com.reachinbox.app.data.model.Email;
import com.reachinbox.app.databinding.ItemEmailBinding;

import java.util.ArrayList;
import java.util.List;

public class EmailAdapter extends RecyclerView.Adapter<EmailAdapter.ViewHolder> {

    private List<Email> emails = new ArrayList<>();
    private final OnEmailClickListener listener;

    public interface OnEmailClickListener {
        void onEmailClick(Email email);
    }

    public EmailAdapter(OnEmailClickListener listener) {
        this.listener = listener;
    }

    public void setEmails(List<Email> newEmails) {
        androidx.recyclerview.widget.DiffUtil.DiffResult result = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return emails.size();
            }

            @Override
            public int getNewListSize() {
                return newEmails.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return java.util.Objects.equals(emails.get(oldItemPosition).getId(), newEmails.get(newItemPosition).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Email oldItem = emails.get(oldItemPosition);
                Email newItem = newEmails.get(newItemPosition);
                return java.util.Objects.equals(oldItem.getStatus(), newItem.getStatus())
                        && java.util.Objects.equals(oldItem.getSubject(), newItem.getSubject())
                        && java.util.Objects.equals(oldItem.getRecipient(), newItem.getRecipient())
                        && java.util.Objects.equals(oldItem.getTextBody(), newItem.getTextBody())
                        && java.util.Objects.equals(oldItem.getCreatedAt(), newItem.getCreatedAt());
            }
        });
        this.emails = new ArrayList<>(newEmails);
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEmailBinding binding = ItemEmailBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Email email = emails.get(position);
        holder.binding.tvRecipient.setText(email.getRecipient());
        holder.binding.tvSubject.setText(email.getSubject());
        holder.binding.tvStatus.setText(email.getStatus());
        holder.binding.tvPreview.setText(email.getTextBody());

        if (email.getSenderAccount() != null) {
            holder.binding.tvSender.setText("via " + email.getSenderAccount().getEmail());
        } else {
            holder.binding.tvSender.setText("");
        }

        if (email.getCreatedAt() != null) {
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    email.getCreatedAt().getTime(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            holder.binding.tvTime.setText(relativeTime);
        } else {
            holder.binding.tvTime.setText(R.string.label_time_unknown);
        }

        // Status styling
        int backgroundRes = R.drawable.bg_badge_neutral;
        int textColorRes = R.color.colorOnNeutralContainer;
        
        if (email.getStatus() != null) {
            switch (email.getStatus()) {
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
                    backgroundRes = R.drawable.bg_badge_info; // Or primary
                    textColorRes = R.color.colorOnInfoContainer;
                    break;
                case "DRAFT":
                    backgroundRes = R.drawable.bg_badge_neutral;
                    textColorRes = R.color.colorOnNeutralContainer;
                    break;
            }
        }
        holder.binding.tvStatus.setBackgroundResource(backgroundRes);
        holder.binding.tvStatus.setTextColor(holder.itemView.getContext().getColor(textColorRes));

        holder.itemView.setOnClickListener(v -> listener.onEmailClick(email));
    }

    @Override
    public int getItemCount() {
        return emails.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemEmailBinding binding;

        public ViewHolder(ItemEmailBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
