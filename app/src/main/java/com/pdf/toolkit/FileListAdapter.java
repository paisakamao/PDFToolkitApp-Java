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
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> {

    private final Context context;
    private final List<FileItem> fileList;

    public FileListAdapter(Context context, List<FileItem> fileList) {
        this.context = context;
        this.fileList = fileList;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem item = fileList.get(position);

        holder.fileName.setText(item.name);

        String fileSize = Formatter.formatShortFileSize(context, item.size);
        String fileDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(item.lastModified));
        holder.fileDetails.setText(fileSize + " | " + fileDate);

        // --- THIS IS THE CORRECTED CLICK LISTENER ---
        holder.container.setOnClickListener(v -> {
            Intent intent = new Intent(context, PdfViewerActivity.class);
            
            // Convert the file path to a proper Uri
            File file = new File(item.path);
            Uri fileUri = Uri.fromFile(file);

            // Use the NEW key and pass the Uri as a string
            intent.putExtra(PdfViewerActivity.EXTRA_FILE_URI, fileUri.toString());
            
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {
        CardView container;
        TextView fileName;
        TextView fileDetails;
        ImageView fileIcon;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.file_item_container); // Assuming the root is a CardView with this ID
            fileName = itemView.findViewById(R.id.textViewFileName);
            fileDetails = itemView.findViewById(R.id.textViewFileDetails);
            fileIcon = itemView.findViewById(R.id.imageViewFileIcon);
        }
    }
}