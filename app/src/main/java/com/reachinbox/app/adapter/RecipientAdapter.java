package com.reachinbox.app.adapter;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.reachinbox.app.R;
import com.reachinbox.app.data.model.CampaignRecipient;
import com.reachinbox.app.databinding.ItemRecipientBinding;

import java.util.ArrayList;
import java.util.List;

public class RecipientAdapter extends RecyclerView.Adapter<RecipientAdapter.ViewHolder> {

    private List<CampaignRecipient> recipients = new ArrayList<>();

    public void setRecipients(List<CampaignRecipient> newRecipients) {
        androidx.recyclerview.widget.DiffUtil.DiffResult result = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return recipients.size();
            }

            @Override
            public int getNewListSize() {
                return newRecipients.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return recipients.get(oldItemPosition).getId().equals(newRecipients.get(newItemPosition).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                CampaignRecipient oldItem = recipients.get(oldItemPosition);
                CampaignRecipient newItem = newRecipients.get(newItemPosition);
                return oldItem.getStatus().equals(newItem.getStatus()) &&
                        (oldItem.getErrorMessage() == null ? newItem.getErrorMessage() == null : oldItem.getErrorMessage().equals(newItem.getErrorMessage()));
            }
        });
        this.recipients = new ArrayList<>(newRecipients);
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRecipientBinding binding = ItemRecipientBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CampaignRecipient recipient = recipients.get(position);
        Context context = holder.itemView.getContext();

        holder.binding.tvRecipientEmail.setText(recipient.getRecipient());
        holder.binding.tvRecipientStatus.setText(recipient.getStatus());

        if (recipient.getErrorMessage() != null) {
            holder.binding.tvError.setVisibility(View.VISIBLE);
            holder.binding.tvError.setText(context.getString(R.string.label_error_prefix, recipient.getErrorMessage()));
        } else {
            holder.binding.tvError.setVisibility(View.GONE);
        }

        if (recipient.getSentAt() != null) {
            holder.binding.tvSentAt.setVisibility(View.VISIBLE);
            String formattedDate = DateFormat.getDateFormat(context).format(recipient.getSentAt()) + ", " + DateFormat.getTimeFormat(context).format(recipient.getSentAt());
            holder.binding.tvSentAt.setText(context.getString(R.string.label_sent_at_prefix, formattedDate));
        } else {
            holder.binding.tvSentAt.setVisibility(View.GONE);
        }

        // Status styling
        int backgroundRes = R.drawable.bg_badge_neutral;
        int textColorRes = R.color.colorOnNeutralContainer;
        
        switch (recipient.getStatus()) {
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
            case "QUEUED":
                backgroundRes = R.drawable.bg_badge_neutral;
                textColorRes = R.color.colorOnNeutralContainer;
                break;
        }
        holder.binding.tvRecipientStatus.setBackgroundResource(backgroundRes);
        holder.binding.tvRecipientStatus.setTextColor(context.getColor(textColorRes));
    }

    @Override
    public int getItemCount() {
        return recipients.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemRecipientBinding binding;

        public ViewHolder(ItemRecipientBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
