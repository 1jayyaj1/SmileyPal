package com.jayyaj.smileypal.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jayyaj.smileypal.R;
import com.jayyaj.smileypal.model.FaceDetection;

import java.util.List;

public class FaceDetectionRecyclerAdapter extends RecyclerView.Adapter<FaceDetectionRecyclerAdapter.ViewHolder> {
    private final Context context;
    private final List<FaceDetection> faceDetectionList;

    public FaceDetectionRecyclerAdapter(Context context, List<FaceDetection> faceDetectionList) {
        this.context = context;
        this.faceDetectionList = faceDetectionList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.face_detection_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FaceDetection faceDetection = faceDetectionList.get(position);
        holder.text1.setText(faceDetection.getText());

    }

    @Override
    public int getItemCount() {
        return faceDetectionList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView text1;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(R.id.row_fd_text1);
        }
    }
}
