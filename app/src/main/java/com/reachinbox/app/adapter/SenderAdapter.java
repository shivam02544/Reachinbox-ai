package com.reachinbox.app.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.content.res.ColorStateList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.reachinbox.app.R;
import com.reachinbox.app.data.model.Sender;
import com.reachinbox.app.databinding.ItemSenderBinding;

import java.util.ArrayList;
import java.util.List;

public class SenderAdapter extends RecyclerView.Adapter<SenderAdapter.ViewHolder> {

    private List<Sender> senders = new ArrayList<>();
    private final OnSenderActionListener listener;

    public interface OnSenderActionListener {
        void onVerify(Sender sender);
        void onDelete(Sender sender);
    }

    public SenderAdapter(OnSenderActionListener listener) {
        this.listener = listener;
    }

    public void setSenders(List<Sender> newSenders) {
        androidx.recyclerview.widget.DiffUtil.DiffResult result = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return senders.size();
            }

            @Override
            public int getNewListSize() {
                return newSenders.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return senders.get(oldItemPosition).getId().equals(newSenders.get(newItemPosition).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Sender oldItem = senders.get(oldItemPosition);
                Sender newItem = newSenders.get(newItemPosition);
                return oldItem.getStatus().equals(newItem.getStatus()) &&
                        (oldItem.getDisplayName() == null ? newItem.getDisplayName() == null : oldItem.getDisplayName().equals(newItem.getDisplayName()));
            }
        });
        this.senders = new ArrayList<>(newSenders);
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSenderBinding binding = ItemSenderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sender sender = senders.get(position);
        holder.binding.tvDisplayName.setText(sender.getDisplayName());
        holder.binding.tvEmail.setText(sender.getEmail());
        holder.binding.tvStatus.setText(sender.getStatus());

        String provider = sender.getProvider();
        int providerIcon = R.drawable.ic_mail;
        if (provider != null) {
            switch (provider.toUpperCase(java.util.Locale.ROOT)) {
                case "GMAIL":
                case "GOOGLE":
                    providerIcon = R.drawable.ic_mail;
                    break;
                case "OUTLOOK":
                case "MICROSOFT":
                    providerIcon = R.drawable.ic_person;
                    break;
                case "SMTP":
                default:
                    providerIcon = R.drawable.ic_send;
                    break;
            }
        }
        holder.binding.ivSenderProvider.setImageResource(providerIcon);
        holder.binding.ivSenderProvider.setImageTintList(ColorStateList.valueOf(
                holder.itemView.getContext().getColor(R.color.md_theme_primary)));

        // Status styling
        int backgroundRes = R.drawable.bg_badge_neutral;
        int textColorRes = R.color.colorOnNeutralContainer;
        
        if (sender.getStatus() != null) {
            switch (sender.getStatus()) {
                case "CONNECTED":
                    backgroundRes = R.drawable.bg_badge_success;
                    textColorRes = R.color.colorOnSuccessContainer;
                    break;
                case "DISCONNECTED":
                    backgroundRes = R.drawable.bg_badge_warning;
                    textColorRes = R.color.colorOnWarningContainer;
                    break;
                case "ERROR":
                    backgroundRes = R.drawable.bg_badge_error;
                    textColorRes = R.color.colorOnErrorContainer;
                    break;
            }
        }
        holder.binding.tvStatus.setBackgroundResource(backgroundRes);
        holder.binding.tvStatus.setTextColor(holder.itemView.getContext().getColor(textColorRes));

        holder.binding.btnVerify.setOnClickListener(v -> listener.onVerify(sender));
        holder.binding.btnDelete.setOnClickListener(v -> listener.onDelete(sender));
    }

    @Override
    public int getItemCount() {
        return senders.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemSenderBinding binding;

        public ViewHolder(ItemSenderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
