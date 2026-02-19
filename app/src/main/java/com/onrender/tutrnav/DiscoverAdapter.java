package com.onrender.tutrnav;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class DiscoverAdapter extends RecyclerView.Adapter<DiscoverAdapter.DiscoverViewHolder> {

    private List<TuitionModel> list;
    private OnTuitionClickListener listener;

    public interface OnTuitionClickListener {
        void onClick(TuitionModel model);
    }

    public DiscoverAdapter(List<TuitionModel> list, OnTuitionClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DiscoverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_discover_card, parent, false);
        return new DiscoverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiscoverViewHolder holder, int position) {
        // Infinite loop logic
        TuitionModel item = list.get(position % list.size());

        holder.title.setText(item.getTitle());
        holder.subtitle.setText(item.getSubject() + " | ₹" + item.getFee());

        // Load actual banner from Cloudinary/Firestore URL
        Glide.with(holder.itemView.getContext())
                .load(item.getBannerUrl())
                .placeholder(R.drawable.bg_gradient_overlay)
                .centerCrop()
                .into(holder.image);

        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    @Override
    public int getItemCount() {
        return list == null || list.isEmpty() ? 0 : Integer.MAX_VALUE;
    }

    static class DiscoverViewHolder extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        ImageView image;

        public DiscoverViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvDiscoverTitle);
            subtitle = itemView.findViewById(R.id.tvDiscoverSubtitle);
            image = itemView.findViewById(R.id.imgDiscover);
        }
    }
}