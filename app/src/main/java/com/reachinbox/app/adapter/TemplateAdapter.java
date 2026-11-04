package com.reachinbox.app.adapter;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.reachinbox.app.R;
import com.reachinbox.app.data.model.Template;
import com.reachinbox.app.databinding.ItemTemplateBinding;

import java.util.ArrayList;
import java.util.List;

public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.ViewHolder> {

    private List<Template> templates = new ArrayList<>();
    private final OnTemplateActionListener listener;

    public interface OnTemplateActionListener {
        void onEdit(Template template);
        void onDelete(Template template);
        void onClick(Template template);
    }

    public TemplateAdapter(OnTemplateActionListener listener) {
        this.listener = listener;
    }

    public void setTemplates(List<Template> templates) {
        this.templates = templates;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTemplateBinding binding = ItemTemplateBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Template template = templates.get(position);
        holder.binding.tvTemplateName.setText(template.getName());
        holder.binding.tvSubject.setText(template.getSubject());
        holder.binding.tvBodyPreview.setText(template.getBodyText());

        if (template.getUpdatedAt() != null) {
            holder.binding.tvUpdated.setText(DateFormat.getDateFormat(holder.itemView.getContext()).format(template.getUpdatedAt()));
        }

        holder.binding.btnEdit.setOnClickListener(v -> listener.onEdit(template));
        holder.binding.btnDelete.setOnClickListener(v -> listener.onDelete(template));
        holder.itemView.setOnClickListener(v -> listener.onClick(template));
    }

    @Override
    public int getItemCount() {
        return templates.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemTemplateBinding binding;

        public ViewHolder(ItemTemplateBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
