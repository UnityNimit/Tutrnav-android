package com.onrender.tutrnav;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private Context context;
    private List<TuitionClass> classList;

    public ScheduleAdapter(Context context, List<TuitionClass> classList) {
        this.context = context;
        this.classList = classList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Points to the NEW Legendary XML
        View view = LayoutInflater.from(context).inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TuitionClass item = classList.get(position);

        // Map Data to the NEW IDs
        holder.tvSubjectName.setText(item.getSubject());
        holder.tvTopic.setText(item.getTopic());
        holder.tvTutorName.setText(item.getTutorName());
        holder.tvTimeStart.setText(item.getStartTime());
        holder.tvDuration.setText(item.getDuration());
        holder.tvLocation.setText(item.getLocation());
        holder.tvStatus.setText(item.getStatus());

        // Dynamic Styling based on Status
        if (item.getStatus().equalsIgnoreCase("CANCELLED")) {
            holder.tvStatus.setTextColor(Color.parseColor("#FF5252")); // Red Text
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_chip_red); // The file we just created
            holder.viewColorBar.setBackgroundColor(Color.parseColor("#FF5252"));
            holder.btnAction.setText("Details");

        } else if (item.getStatus().equalsIgnoreCase("LIVE")) {
            holder.tvStatus.setTextColor(Color.parseColor("#00E676")); // Green Text
            holder.viewColorBar.setBackgroundColor(Color.parseColor("#00E676"));
            // If you don't have bg_status_chip_green, use default or create it
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_chip);
            holder.btnAction.setText("Join");

        } else {
            // Normal / Upcoming
            holder.tvStatus.setTextColor(Color.parseColor("#FFCA28")); // Gold Text
            holder.viewColorBar.setBackgroundColor(item.getColorCode());
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_chip);
            holder.btnAction.setText("Map");
        }

        // Placeholder Image
        holder.imgTutor.setImageResource(R.mipmap.ic_launcher);

        holder.btnAction.setOnClickListener(v -> {
            Toast.makeText(context, "Clicked: " + item.getSubject(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return classList.size();
    }

    // The ViewHolder MUST match item_schedule.xml IDs
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubjectName, tvTopic, tvTutorName, tvTimeStart, tvDuration, tvLocation, tvStatus;
        View viewColorBar;
        ImageView imgTutor;
        MaterialButton btnAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubjectName = itemView.findViewById(R.id.tvSubjectName);
            tvTopic = itemView.findViewById(R.id.tvTopic);
            tvTutorName = itemView.findViewById(R.id.tvTutorName);
            tvTimeStart = itemView.findViewById(R.id.tvTimeStart);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            viewColorBar = itemView.findViewById(R.id.viewColorBar);
            imgTutor = itemView.findViewById(R.id.imgTutor);
            btnAction = itemView.findViewById(R.id.btnAction);
        }
    }
}