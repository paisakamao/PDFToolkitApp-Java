package com.pdf.toolkit;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> {

    private final List<FileItem> files;
    private final OnFileClickListener listener;

    public interface OnFileClickListener {
        void onFileClick(FileItem item);
    }

    public FileListAdapter(List<FileItem> files, OnFileClickListener listener) {
        this.files = files;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem file = files.get(position);
        holder.bind(file, listener);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView fileIcon;
        TextView fileName;
        TextView fileDetails;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.icon_file_type);
            fileName = itemView.findViewById(R.id.text_file_name);
            fileDetails = itemView.findViewById(R.id.text_file_details);
        }

        public void bind(final FileItem item, final OnFileClickListener listener) {
            fileName.setText(item.name);
            fileDetails.setText(String.format("%s - %s", formatFileSize(item.size), formatDate(item.date)));
            fileIcon.setImageResource(R.drawable.ic_pdflist);
            itemView.setOnClickListener(v -> listener.onFileClick(item));
        }

        private static String formatDate(long millis) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            return sdf.format(new Date(millis));
        }

        private static String formatFileSize(long size) {
            if (size < 1024) return size + " B";
            int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
            return String.format(Locale.US, "%.1f %sB", (double) size / (1L << (z * 10)), " KMGTPE".charAt(z));
        }
    }
}
