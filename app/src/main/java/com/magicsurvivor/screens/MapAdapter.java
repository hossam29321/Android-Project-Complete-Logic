package com.magicsurvivor.screens;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.magicsurvivor.R;
import java.util.List;

public class MapAdapter extends RecyclerView.Adapter<MapAdapter.MapViewHolder> {
    private List<MapItem> maps;
    private Context context;
    private OnMapSelectedListener listener;
    private int selectedPosition = -1;

    public interface OnMapSelectedListener {
        void onMapSelected(int position);
    }

    public MapAdapter(Context context, List<MapItem> maps, OnMapSelectedListener listener) {
        this.context = context;
        this.maps = maps;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_map, parent, false);
        return new MapViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MapViewHolder holder, int position) {
        MapItem map = maps.get(position);
        
        // Set image based on whether it's a custom image or resource drawable
        if (map.isCustomImage() && map.getImageUri() != null) {
            holder.mapImage.setImageURI(map.getImageUri());
        } else {
            holder.mapImage.setImageResource(map.getImageResId());
        }
        
        holder.mapName.setText(map.getName());
        holder.mapDescription.setText(map.getDescription());

        // Highlight selected map
        if (position == selectedPosition) {
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.selectionGray));
        } else {
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.transparent));
        }

        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = position;
            listener.onMapSelected(position);
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return maps.size();
    }

    public static class MapViewHolder extends RecyclerView.ViewHolder {
        ImageView mapImage;
        TextView mapName;
        TextView mapDescription;

        public MapViewHolder(@NonNull View itemView) {
            super(itemView);
            mapImage = itemView.findViewById(R.id.mapImage);
            mapName = itemView.findViewById(R.id.mapName);
            mapDescription = itemView.findViewById(R.id.mapDescription);
        }
    }
}
