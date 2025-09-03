package com.pdfscanner.toolkit;
// (This file is complete and correct from our previous working version)
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
public class ThumbnailAdapter extends RecyclerView.Adapter<ThumbnailAdapter.ViewHolder> {
    private final ArrayList<String> imagePaths; private final OnThumbnailListener listener;
    public interface OnThumbnailListener { void onThumbnailClick(int position); }
    public ThumbnailAdapter(ArrayList<String> imagePaths, OnThumbnailListener listener) { this.imagePaths = imagePaths; this.listener = listener; }
    @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) { View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_thumbnail, parent, false); return new ViewHolder(view); }
    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) { File imgFile = new File(imagePaths.get(position)); if(imgFile.exists()){ holder.thumbnailImage.setImageURI(Uri.fromFile(imgFile)); } }
    @Override public int getItemCount() { return imagePaths.size(); }
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView thumbnailImage;
        public ViewHolder(@NonNull View itemView) { super(itemView); thumbnailImage = itemView.findViewById(R.id.thumbnail_image); itemView.setOnClickListener(this); }
        @Override public void onClick(View v) { listener.onThumbnailClick(getAdapterPosition()); }
    }
}
