package com.pdf.toolkit;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllFilesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FileListAdapter adapter;
    private final List<FileItem> fileList = new ArrayList<>();
    private View permissionView;

    // --- All your permission launchers remain the same ---
    private final ActivityResultLauncher<Intent> requestAllFilesAccessLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> checkPermissionAndLoadFiles());

    private final ActivityResultLauncher<String> requestLegacyPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    loadFilesFromStorage();
                } else {
                    showPermissionNeededUI();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_files);

        recyclerView = findViewById(R.id.recycler_view_files);
        permissionView = findViewById(R.id.permission_needed_view);
        Button grantPermissionButton = findViewById(R.id.btn_grant_permission);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // --- START: THIS IS THE MAJOR CHANGE ---
        // We now pass a "click listener" to the adapter.
        // This is the code that will run when a user taps on a file in the list.
        adapter = new FileListAdapter(fileList, fileItem -> {
            if (fileItem.name != null && fileItem.name.toLowerCase().endsWith(".pdf")) {
                // If the file is a PDF, open it with our new IN-APP viewer.
                openPdfInApp(fileItem);
            } else {
                // If it's anything else (like a .doc or .txt), open it with an EXTERNAL app.
                openFileExternally(fileItem);
            }
        });
        // --- END: THIS IS THE MAJOR CHANGE ---
        
        recyclerView.setAdapter(adapter);
        grantPermissionButton.setOnClickListener(v -> checkPermissionAndLoadFiles());
    }

    // --- START: NEW FUNCTIONS TO OPEN FILES ---
    private void openPdfInApp(FileItem item) {
        Intent intent = new Intent(this, PdfViewerActivity.class);
        // We send the filename to our new PdfViewerActivity
        intent.putExtra(PdfViewerActivity.EXTRA_FILE_NAME, item.name);
        startActivity(intent);
    }

    private void openFileExternally(FileItem item) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadsDir, item.name);

        if (!file.exists()) {
            Toast.makeText(this, "Error: File not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use the FileProvider to get a secure URI, which is required for sharing files.
        Uri uri = FileProvider.getUriForFile(this, "com.pdf.toolkit.fileprovider", file);
        
        // Create an Intent to view the file
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String mimeType = getMimeType(item.name);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Grant permission to the other app

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app found to open this file type.", Toast.LENGTH_LONG).show();
        }
    }
    
    // Helper function to figure out the file's MIME type
    private String getMimeType(String fileName) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
        if (extension != null) {
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (mime != null) return mime;
        }
        return "application/octet-stream"; // A generic fallback
    }
    // --- END: NEW FUNCTIONS TO OPEN FILES ---


    // --- The rest of your code remains the same as it was, it is already correct ---
    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionAndLoadFiles();
    }

    private void checkPermissionAndLoadFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                showFileListUI();
                loadFilesFromStorage();
            } else {
                showPermissionNeededUI();
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                requestAllFilesAccessLauncher.launch(intent);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                showFileListUI();
                loadFilesFromStorage();
            } else {
                showPermissionNeededUI();
                requestLegacyPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void loadFilesFromStorage() {
        new Thread(() -> {
            fileList.clear();
            Uri uri = MediaStore.Files.getContentUri("external");
            String[] projection = {
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_MODIFIED
            };
            String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC";

            try (Cursor cursor = getContentResolver().query(uri, projection, null, null, sortOrder)) {
                if (cursor != null) {
                    int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                    int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
                    int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);

                    while (cursor.moveToNext()) {
                        String name = cursor.getString(nameColumn);
                        if (name != null && (name.toLowerCase().endsWith(".pdf") || name.toLowerCase().endsWith(".doc") || name.toLowerCase().endsWith(".docx") || name.toLowerCase().endsWith(".txt"))) {
                           long size = cursor.getLong(sizeColumn);
                           long date = cursor.getLong(dateColumn);
                           fileList.add(new FileItem(name, size, date * 1000));
                        }
                    }
                }
            }
            runOnUiThread(() -> adapter.notifyDataSetChanged());
        }).start();
    }

    private void showFileListUI() {
        recyclerView.setVisibility(View.VISIBLE);
        permissionView.setVisibility(View.GONE);
    }

    private void showPermissionNeededUI() {
        recyclerView.setVisibility(View.GONE);
        permissionView.setVisibility(View.VISIBLE);
    }
    
    public static class FileItem { String name; long size; long date; public FileItem(String n, long s, long d){name=n;size=s;date=d;}}
    
    // --- START: UPDATED FILE LIST ADAPTER ---
    // This adapter is now designed to handle clicks.
    public static class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> {
        private final List<FileItem> files;
        private final OnFileClickListener listener;

        // An interface is the standard way to handle clicks in a list
        public interface OnFileClickListener {
            void onFileClick(FileItem item);
        }

        public FileListAdapter(List<FileItem> files, OnFileClickListener listener) {
            this.files = files;
            this.listener = listener;
        }

        @Override
        public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
            return new FileViewHolder(view);
        }

        @Override
        public void onBindViewHolder(FileViewHolder holder, int position) {
            FileItem file = files.get(position);
            holder.bind(file, listener);
        }

        @Override
        public int getItemCount() {
            return files.size();
        }
        
        private String formatDate(long millis) {
            return new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(new Date(millis));
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

            public void bind(final FileItem item, final OnFileClickListener listener) {
                fileName.setText(item.name);
                fileDetails.setText(formatFileSize(item.size) + " - " + formatDate(item.date));
                if (item.name.toLowerCase().endsWith(".pdf")) {
                    fileIcon.setImageResource(android.R.drawable.ic_menu_gallery); // Placeholder PDF icon
                } else {
                    fileIcon.setImageResource(android.R.drawable.ic_menu_edit); // Placeholder DOC icon
                }
                // This is where the click is registered for the whole row
                itemView.setOnClickListener(v -> listener.onFileClick(item));
            }
        }
    }
    // --- END: UPDATED FILE LIST ADAPTER ---
}
