package com.pdf.toolkit;

// (All necessary imports)
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import android.text.format.Formatter;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> {

    private final List<FileItem> files;
    private final OnFileClickListener listener;
    private boolean isMultiSelectMode = false;
    private final Set<FileItem> selectedItems = new HashSet<>();

    public interface OnFileClickListener {
        void onFileClick(FileItem item);
        void onFileLongClick();
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
        holder.bind(file);
        holder.itemView.setActivated(selectedItems.contains(file));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    // --- Selection Management ---
    public boolean isMultiSelectMode() { return isMultiSelectMode; }
    public void setMultiSelectMode(boolean multiSelectMode) {
        this.isMultiSelectMode = multiSelectMode;
        if (!multiSelectMode) {
            selectedItems.clear();
        }
        notifyDataSetChanged();
    }
    public void toggleSelection(int position) {
        FileItem item = files.get(position);
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        } else {
            selectedItems.add(item);
        }
        notifyItemChanged(position);
    }
    public List<FileItem> getSelectedItems() { return new ArrayList<>(selectedItems); }
    public int getSelectedItemCount() { return selectedItems.size(); }

    public class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileName, fileSize, fileDate;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.text_file_name);
            fileSize = itemView.findViewById(R.id.text_file_size);
            fileDate = itemView.findViewById(R.id.text_file_date);
        }

        public void bind(final FileItem item) {
            fileName.setText(item.name);
            fileSize.setText(Formatter.formatShortFileSize(itemView.getContext(), item.size));
            fileDate.setText(new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(new Date(item.date)));

            itemView.setOnClickListener(v -> {
                if (isMultiSelectMode) {
                    listener.onFileClick(item);
                } else {
                    listener.onFileClick(item);
                }
            });
            itemView.setOnLongClickListener(v -> {
                listener.onFileLongClick();
                return true;
            });
        }
    }
}