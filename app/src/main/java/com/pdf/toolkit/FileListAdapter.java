package com.pdf.toolkit;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.format.Formatter;
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
        TextView fileDetails; // This will now be for the DATE ONLY
        TextView fileSize;   // The new TextView for the file size

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.icon_file_type);
            fileName = itemView.findViewById(R.id.text_file_name);
            fileDetails = itemView.findViewById(R.id.text_file_details);
            fileSize = itemView.findViewById(R.id.text_file_size); // Find the new TextView
        }

        public void bind(final FileItem item, final OnFileClickListener listener) {
            fileName.setText(item.name);
            
            // Set the file size text on its own TextView
            fileSize.setText(Formatter.formatShortFileSize(itemView.getContext(), item.size));
            
            // Set the date text (formatted as you requested) on the other TextView
            fileDetails.setText(formatDate(item.date));

            fileIcon.setImageResource(R.drawable.ic_pdflist);
            
            itemView.setOnClickListener(v -> listener.onFileClick(item));
        }

        // 4. Date formatted with time and no year
        private static String formatDate(long millis) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            return sdf.format(new Date(millis));
        }
    }
}