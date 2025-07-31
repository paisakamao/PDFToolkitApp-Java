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
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AllFilesActivity extends AppCompatActivity {
    
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout permissionView;
    private FileListAdapter adapter;
    private final List<FileItem> fileList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_files);

        recyclerView = findViewById(R.id.recycler_view_files);
        progressBar = findViewById(R.id.progress_bar);
        permissionView = findViewById(R.id.permission_needed_view);
        Button grantButton = findViewById(R.id.btn_grant_permission);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileListAdapter(this, fileList);
        recyclerView.setAdapter(adapter);

        grantButton.setOnClickListener(v -> requestStoragePermission());
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndLoadFiles();
    }

    private void checkAndLoadFiles() {
        if (hasStoragePermission()) {
            permissionView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            loadPdfFiles(); // This will now be called every time you enter the screen
        } else {
            permissionView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void loadPdfFiles() {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            List<FileItem> freshFileList = new ArrayList<>();
            
            // --- THIS IS THE CORRECTED QUERY ---
            String[] projection = { MediaStore.Files.FileColumns.DISPLAY_name, MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DATE_MODIFIED };
            
            // This selection finds PDFs and filters out trashed files on modern Android
            String selection = MediaStore.Files.FileColumns.MIME_TYPE + " = ? AND " + MediaStore.MediaColumns.IS_TRASHED + " = 0";
            String[] selectionArgs = new String[]{"application/pdf"};
            Uri queryUri = MediaStore.Files.getContentUri("external");

            try (Cursor cursor = getContentResolver().query(queryUri, projection, selection, selectionArgs, null)) {
                if (cursor != null) {
                    int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                    int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
                    int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
                    int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);

                    while (cursor.moveToNext()) {
                        freshFileList.add(new FileItem(cursor.getString(nameColumn), cursor.getString(pathColumn), cursor.getLong(sizeColumn), cursor.getLong(dateColumn) * 1000));
                    }
                }
            }
            
            Collections.sort(freshFileList, (f1, f2) -> Long.compare(f2.lastModified, f1.lastModified));
            
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                fileList.clear(); // Clear the old list
                fileList.addAll(freshFileList); // Add the fresh list
                adapter.notifyDataSetChanged(); // This correctly forces the UI to refresh
            });
        }).start();
    }
    
    // (Permission checking methods remain the same)
    private boolean hasStoragePermission() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { return Environment.isExternalStorageManager(); } else { return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED; } }
    private void requestStoragePermission() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { try { Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION); intent.addCategory("android.intent.category.DEFAULT"); intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName()))); startActivity(intent); } catch (Exception e) { Intent intent = new Intent(); intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION); startActivity(intent); } } }
}
