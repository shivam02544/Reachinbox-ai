package com.reachinbox.app.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.reachinbox.app.R;
import com.reachinbox.app.data.model.Campaign;
import com.reachinbox.app.data.model.CampaignStats;
import com.reachinbox.app.databinding.ItemCampaignBinding;

import java.util.ArrayList;
import java.util.List;

public class CampaignAdapter extends RecyclerView.Adapter<CampaignAdapter.ViewHolder> {

    private List<Campaign> campaigns = new ArrayList<>();
    private final OnCampaignClickListener listener;

    public interface OnCampaignClickListener {
        void onCampaignClick(Campaign campaign);
    }

    public CampaignAdapter(OnCampaignClickListener listener) {
        this.listener = listener;
    }

    public void setCampaigns(List<Campaign> newCampaigns) {
        androidx.recyclerview.widget.DiffUtil.DiffResult result = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return campaigns.size();
            }

            @Override
            public int getNewListSize() {
                return newCampaigns.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return campaigns.get(oldItemPosition).getId().equals(newCampaigns.get(newItemPosition).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Campaign oldItem = campaigns.get(oldItemPosition);
                Campaign newItem = newCampaigns.get(newItemPosition);
                return oldItem.getStatus().equals(newItem.getStatus()) &&
                        oldItem.getName().equals(newItem.getName());
            }
        });
        this.campaigns = new ArrayList<>(newCampaigns);
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCampaignBinding binding = ItemCampaignBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Campaign campaign = campaigns.get(position);
        Context context = holder.itemView.getContext();

        holder.binding.tvCampaignName.setText(campaign.getName());
        holder.binding.tvSubject.setText(campaign.getSubject());
        holder.binding.tvStatus.setText(campaign.getStatus());

        if (campaign.getSenderAccount() != null) {
            holder.binding.tvSender.setText("via " + campaign.getSenderAccount().getEmail());
        }

        if (campaign.getCreatedAt() != null) {
            holder.binding.tvDate.setText(DateFormat.getDateFormat(context).format(campaign.getCreatedAt()));
        }

        // Stats and Progress
        CampaignStats stats = campaign.getStats();
        if (stats != null) {
            holder.binding.progressContainer.setVisibility(View.VISIBLE);
            int total = stats.getTotal();
            int sent = stats.getSent();
            int failed = stats.getFailed();
            int pending = stats.getPending() + stats.getQueued() + stats.getProcessing();

            holder.binding.progressIndicator.setMax(total);
            holder.binding.progressIndicator.setProgress(sent + failed);
            
            int percent = total > 0 ? (int) (((float) (sent + failed) / total) * 100) : 0;
            holder.binding.tvProgressPercent.setText(percent + "%");
            holder.binding.tvProgressText.setText(sent + " / " + total + " sent");
            holder.binding.tvSentCount.setText("✓ " + sent + " Sent");
            holder.binding.tvFailedCount.setText("× " + failed + " Failed");
            holder.binding.tvPendingCount.setText("◷ " + pending + " Pending");
        } else {
            holder.binding.progressContainer.setVisibility(View.GONE);
        }

        // Status styling
        int backgroundRes = R.drawable.bg_badge_neutral;
        int textColorRes = R.color.colorOnNeutralContainer;
        
        switch (campaign.getStatus()) {
            case "DRAFT":
                backgroundRes = R.drawable.bg_badge_neutral;
                textColorRes = R.color.colorOnNeutralContainer;
                break;
            case "READY":
                backgroundRes = R.drawable.bg_badge_info;
                textColorRes = R.color.colorOnInfoContainer;
                break;
            case "RUNNING":
                backgroundRes = R.drawable.bg_badge_info;
                textColorRes = R.color.colorOnInfoContainer;
                break;
            case "PAUSED":
                backgroundRes = R.drawable.bg_badge_warning;
                textColorRes = R.color.colorOnWarningContainer;
                break;
            case "COMPLETED":
                backgroundRes = R.drawable.bg_badge_success;
                textColorRes = R.color.colorOnSuccessContainer;
                break;
            case "CANCELLED":
                backgroundRes = R.drawable.bg_badge_neutral;
                textColorRes = R.color.colorOnNeutralContainer;
                break;
            case "FAILED":
                backgroundRes = R.drawable.bg_badge_error;
                textColorRes = R.color.colorOnErrorContainer;
                break;
        }
        holder.binding.tvStatus.setBackgroundResource(backgroundRes);
        holder.binding.tvStatus.setTextColor(context.getColor(textColorRes));

        holder.itemView.setOnClickListener(v -> listener.onCampaignClick(campaign));
    }

    @Override
    public int getItemCount() {
        return campaigns.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemCampaignBinding binding;

        public ViewHolder(ItemCampaignBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
