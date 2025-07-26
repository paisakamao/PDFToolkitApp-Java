package com.pdf.toolkit;

import android.Manifest;
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
import android.widget.Button;
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
    private View permissionView; // A view to show if permission is denied

    // --- NEW: Launcher for the modern "All Files Access" settings screen ---
    private final ActivityResultLauncher<Intent> requestAllFilesAccessLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // After the user returns from the settings screen, check the permission again.
                checkPermissionAndLoadFiles();
            });

    // --- This is for old Android versions ---
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
        setContentView(R.layout.activity_all_files); // We will update this layout

        recyclerView = findViewById(R.id.recycler_view_files);
        permissionView = findViewById(R.id.permission_needed_view);
        Button grantPermissionButton = findViewById(R.id.btn_grant_permission);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileListAdapter(fileList);
        recyclerView.setAdapter(adapter);
        
        grantPermissionButton.setOnClickListener(v -> checkPermissionAndLoadFiles());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check permission every time the user returns to this screen
        checkPermissionAndLoadFiles();
    }

    private void checkPermissionAndLoadFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11 and above
            if (Environment.isExternalStorageManager()) {
                // We have the "All Files Access" permission
                showFileListUI();
                loadFilesFromStorage();
            } else {
                // We don't have it, so we need to ask for it by sending the user to a settings screen.
                showPermissionNeededUI();
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                requestAllFilesAccessLauncher.launch(intent);
            }
        } else { // Old Android versions (10 and below)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                showFileListUI();
                loadFilesFromStorage();
            } else {
                // Ask for the old permission directly.
                showPermissionNeededUI();
                requestLegacyPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void loadFilesFromStorage() {
        // This is a background thread to prevent the app from freezing while searching for files.
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
                        // A simple filter to show common document types
                        if (name != null && (name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".txt"))) {
                           long size = cursor.getLong(sizeColumn);
                           long date = cursor.getLong(dateColumn);
                           fileList.add(new FileItem(name, size, date * 1000));
                        }
                    }
                }
            }
            // Update the UI on the main thread
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
    
    // The FileItem and FileListAdapter classes remain the same as before...
    public static class FileItem { String name; long size; long date; public FileItem(String n, long s, long d){name=n;size=s;date=d;}}
    public static class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> {
        private final List<FileItem> files;
        public FileListAdapter(List<FileItem> files){this.files=files;}
        @Override public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType){View v=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file,parent,false);return new FileViewHolder(v);}
        @Override public void onBindViewHolder(FileViewHolder h, int p){FileItem f=files.get(p);h.fileName.setText(f.name);h.fileDetails.setText(formatFileSize(f.size)+" - "+formatDate(f.date));if(f.name.endsWith(".pdf")){h.fileIcon.setImageResource(android.R.drawable.ic_menu_gallery);}else{h.fileIcon.setImageResource(android.R.drawable.ic_menu_edit);}}
        @Override public int getItemCount(){return files.size();}
        private String formatDate(long ms){return new SimpleDateFormat("MM/dd/yyyy",Locale.getDefault()).format(new Date(ms));}
        private String formatFileSize(long s){if(s<1024)return s+" B";int z=(63-Long.numberOfLeadingZeros(s))/10;return String.format(Locale.US,"%.1f %sB",(double)s/(1L<<(z*10))," KMGTPE".charAt(z));}
        public static class FileViewHolder extends RecyclerView.ViewHolder{ImageView fileIcon;TextView fileName;TextView fileDetails;public FileViewHolder(View i){super(i);fileIcon=i.findViewById(R.id.icon_file_type);fileName=i.findViewById(R.id.text_file_name);fileDetails=i.findViewById(R.id.text_file_details);}}
    }
}
