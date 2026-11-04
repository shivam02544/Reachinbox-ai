package com.reachinbox.app.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.reachinbox.app.R;
import com.reachinbox.app.data.model.RecentActivity;
import com.reachinbox.app.databinding.ItemActivityBinding;

import java.util.ArrayList;
import java.util.List;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {

    private List<RecentActivity> activities = new ArrayList<>();
    private final OnActivityClickListener listener;

    public interface OnActivityClickListener {
        void onActivityClick(RecentActivity activity);
    }

    public ActivityAdapter(OnActivityClickListener listener) {
        this.listener = listener;
    }

    public void setActivities(List<RecentActivity> newActivities) {
        androidx.recyclerview.widget.DiffUtil.DiffResult result = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return activities.size();
            }

            @Override
            public int getNewListSize() {
                return newActivities.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return java.util.Objects.equals(activities.get(oldItemPosition).getId(), newActivities.get(newItemPosition).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                RecentActivity oldItem = activities.get(oldItemPosition);
                RecentActivity newItem = newActivities.get(newItemPosition);
                return java.util.Objects.equals(oldItem.getMessage(), newItem.getMessage())
                        && java.util.Objects.equals(oldItem.getType(), newItem.getType())
                        && java.util.Objects.equals(oldItem.getCreatedAt(), newItem.getCreatedAt());
            }
        });
        this.activities = new ArrayList<>(newActivities);
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemActivityBinding binding = ItemActivityBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentActivity activity = activities.get(position);
        holder.binding.tvActivityMessage.setText(activity.getMessage());
        
        CharSequence relativeTime = activity.getCreatedAt() == null
                ? holder.itemView.getContext().getString(R.string.label_time_unknown)
                : DateUtils.getRelativeTimeSpanString(
                        activity.getCreatedAt().getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS);
        holder.binding.tvActivityTime.setText(relativeTime);

        // Set icon based on type
        int iconRes = R.drawable.ic_activity;
        int colorRes = R.color.md_theme_primary;

        if (activity.getType() != null) {
            switch (activity.getType()) {
                case "EMAIL_SENT":
                    iconRes = R.drawable.ic_send;
                    colorRes = R.color.colorSuccess;
                    break;
                case "EMAIL_FAILED":
                    iconRes = R.drawable.ic_warning;
                    colorRes = R.color.colorError;
                    break;
                case "EMAIL_SCHEDULED":
                    iconRes = R.drawable.ic_schedule;
                    colorRes = R.color.md_theme_primary;
                    break;
                case "SENDER_CONNECTED":
                    iconRes = R.drawable.ic_person;
                    colorRes = R.color.colorSuccess;
                    break;
                case "SENDER_DISCONNECTED":
                    iconRes = R.drawable.ic_sender_disconnected;
                    colorRes = R.color.colorNeutral;
                    break;
                case "CAMPAIGN_CREATED":
                case "CAMPAIGN_STARTED":
                    iconRes = R.drawable.ic_campaign;
                    colorRes = R.color.colorInfo;
                    break;
            }
        }
        
        holder.binding.ivActivityIcon.setImageResource(iconRes);
        holder.binding.ivActivityIcon.setImageTintList(
                holder.itemView.getContext().getColorStateList(colorRes));

        holder.itemView.setOnClickListener(v -> listener.onActivityClick(activity));
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemActivityBinding binding;

        public ViewHolder(ItemActivityBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
