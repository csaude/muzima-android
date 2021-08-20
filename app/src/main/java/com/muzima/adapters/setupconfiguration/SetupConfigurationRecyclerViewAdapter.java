package com.muzima.adapters.setupconfiguration;

import android.content.Context;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.muzima.R;
import com.muzima.api.model.SetupConfiguration;

import java.util.ArrayList;
import java.util.List;

public class SetupConfigurationRecyclerViewAdapter extends RecyclerView.Adapter<SetupConfigurationRecyclerViewAdapter.ViewHolder> {
    private Context context;
    private List<SetupConfiguration> setupConfigurationList;
    private List<SetupConfiguration> itemsCopy = new ArrayList<>();
    private String selectedConfigurationUuid;
    private OnSetupConfigurationClickedListener onSetupConfigurationClickedListener;

    public SetupConfigurationRecyclerViewAdapter(Context context, List<SetupConfiguration> setupConfigurationList, OnSetupConfigurationClickedListener clickedListener) {
        this.context = context;
        this.setupConfigurationList = setupConfigurationList;
        this.onSetupConfigurationClickedListener = clickedListener;
        setItemsCopy(setupConfigurationList);
    }

    public void setSelectedConfigurationUuid(String selectedConfigurationUuid) {
        this.selectedConfigurationUuid = selectedConfigurationUuid;
    }

    public String getSelectedConfigurationUuid() {
        return selectedConfigurationUuid;
    }

    public void setItemsCopy(List<SetupConfiguration> itemsCopy) {
        this.itemsCopy = itemsCopy;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_setup_configs_list, parent, false);
        return new ViewHolder(view, onSetupConfigurationClickedListener);
    }

    @Override
    public void onBindViewHolder(@NonNull SetupConfigurationRecyclerViewAdapter.ViewHolder holder, int position) {
        SetupConfiguration setupConfiguration = setupConfigurationList.get(position);
        holder.nameTextView.setText(setupConfiguration.getName());
        holder.descriptionTextView.setText(setupConfiguration.getDescription());
        if (setupConfiguration.getUuid().equalsIgnoreCase(selectedConfigurationUuid))
            holder.cardView.setCardBackgroundColor(context.getResources().getColor(R.color.primary_blue));
        else {
            if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_light_mode), false)) {
                holder.cardView.setCardBackgroundColor(context.getResources().getColor(R.color.primary_black));
            } else
                holder.cardView.setCardBackgroundColor(context.getResources().getColor(R.color.primary_background));
        }
    }

    @Override
    public int getItemCount() {
        return setupConfigurationList.size();
    }

    public void filterItems(String searchTerm) {
        List<SetupConfiguration> filteredSetupConfigurations = new ArrayList<>();
        for (SetupConfiguration setupConfiguration : itemsCopy) {
            if (setupConfiguration.getName() != null && setupConfiguration.getName().toLowerCase().contains(searchTerm.toLowerCase())
                    || setupConfiguration.getDescription() != null && setupConfiguration.getDescription().toLowerCase().contains(searchTerm.toLowerCase())) {
                filteredSetupConfigurations.add(setupConfiguration);
            }
        }

        if (!filteredSetupConfigurations.isEmpty()) {
            setupConfigurationList.clear();
            notifyDataSetChanged();
            setupConfigurationList.addAll(filteredSetupConfigurations);
            notifyDataSetChanged();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final MaterialCardView cardView;
        private final TextView nameTextView;
        private final TextView descriptionTextView;
        private final OnSetupConfigurationClickedListener clickedListener;

        public ViewHolder(@NonNull View itemView, OnSetupConfigurationClickedListener clickedListener) {
            super(itemView);
            this.cardView = itemView.findViewById(R.id.setup_configuration_container);
            this.nameTextView = itemView.findViewById(R.id.config_name);
            this.descriptionTextView = itemView.findViewById(R.id.config_description);
            this.clickedListener = clickedListener;

            this.cardView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            this.clickedListener.onSetupConfigClicked(getAdapterPosition());
        }
    }

    public interface OnSetupConfigurationClickedListener {
        void onSetupConfigClicked(int position);
    }
}