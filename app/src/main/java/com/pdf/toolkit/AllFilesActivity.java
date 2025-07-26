package com.pdf.toolkit;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllFilesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FileListAdapter adapter;
    private final List<FileItem> fileList = new ArrayList<>();

    // Launcher for the permission request
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    loadFilesFromStorage();
                } else {
                    Toast.makeText(this, "Permission denied. Cannot show files.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_files);

        recyclerView = findViewById(R.id.recycler_view_files);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileListAdapter(fileList);
        recyclerView.setAdapter(adapter);

        checkPermissionAndLoadFiles();
    }

    private void checkPermissionAndLoadFiles() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            loadFilesFromStorage();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void loadFilesFromStorage() {
        fileList.clear(); // Clear the list before loading new files
        
        Uri uri = MediaStore.Files.getContentUri("external");
        String[] projection = {
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED
        };
        
        // Query for documents (PDF, DOC, etc.)
        String selection = MediaStore.Files.FileColumns.MIME_TYPE + " = ? OR " + MediaStore.Files.FileColumns.MIME_TYPE + " = ?";
        String[] selectionArgs = new String[]{"application/pdf", "application/msword"}; // Add more types if you want

        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC")) {
            if (cursor != null) {
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
                int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);

                while (cursor.moveToNext()) {
                    String name = cursor.getString(nameColumn);
                    long size = cursor.getLong(sizeColumn);
                    long date = cursor.getLong(dateColumn);
                    fileList.add(new FileItem(name, size, date * 1000)); // date is in seconds, convert to ms
                }
            }
        }
        
        adapter.notifyDataSetChanged(); // Tell the list to refresh
    }
    
    // Simple data class to hold file information
    public static class FileItem {
        String name;
        long size;
        long date;
        public FileItem(String name, long size, long date) {
            this.name = name;
            this.size = size;
            this.date = date;
        }
    }

    // The Adapter that manages the list data
    public static class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> {
        private final List<FileItem> files;

        public FileListAdapter(List<FileItem> files) {
            this.files = files;
        }

        @Override
        public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
            return new FileViewHolder(view);
        }

        @Override
        public void onBindViewHolder(FileViewHolder holder, int position) {
            FileItem file = files.get(position);
            holder.fileName.setText(file.name);
            holder.fileDetails.setText(formatFileSize(file.size) + " - " + formatDate(file.date));
            // You can set different icons based on file type here
            if (file.name.endsWith(".pdf")) {
                holder.fileIcon.setImageResource(android.R.drawable.ic_media_play); // Placeholder icon
            } else {
                holder.fileIcon.setImageResource(android.R.drawable.ic_menu_gallery); // Placeholder icon
            }
        }

        @Override
        public int getItemCount() {
            return files.size();
        }
        
        private String formatDate(long millis) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            return sdf.format(new Date(millis));
        }

        private String formatFileSize(long size) {
            if (size < 1024) return size + " B";
            int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
            return String.format(Locale.US, "%.1f %sB", (double) size / (1L << (z * 10)), " KMGTPE".charAt(z));
        }

        public static class FileViewHolder extends RecyclerView.ViewHolder {
            ImageView fileIcon;
            TextView fileName;
            TextView fileDetails;
            public FileViewHolder(View itemView) {
                super(itemView);
                fileIcon = itemView.findViewById(R.id.icon_file_type);
                fileName = itemView.findViewById(R.id.text_file_name);
                fileDetails = itemView.findViewById(R.id.text_file_details);
            }
        }
    }
}
