package com.pdf.toolkit;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
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

        holder.container.setOnClickListener(v -> {
            Intent intent = new Intent(context, PdfViewerActivity.class);
            File file = new File(item.path);

            // Use FileProvider for safe URI creation
            Uri fileUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
            
            // Grant temporary read permission to the viewer
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
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

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.file_item_container);
            fileName = itemView.findViewById(R.id.text_file_name);
            fileDetails = itemView.findViewById(R.id.text_file_details);
        }
    }
}