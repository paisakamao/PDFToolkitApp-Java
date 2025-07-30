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
    private LinearLayout permissionView; // Changed from RelativeLayout to LinearLayout to match your XML
    private FileListAdapter adapter;
    private List<FileItem> fileList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_files);

        // --- THESE IDs NOW MATCH YOUR XML FILE ---
        recyclerView = findViewById(R.id.recycler_view_files);
        progressBar = findViewById(R.id.progress_bar);
        permissionView = findViewById(R.id.permission_needed_view);
        Button grantButton = findViewById(R.id.btn_grant_permission); // Corrected ID

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
            loadFiles();
        } else {
            permissionView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            // For older Android versions, you would also need to request READ_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        } else {
            // For older Android SDKs, you would use ActivityCompat.requestPermissions here.
        }
    }

    private void loadFiles() {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            fileList.clear();
            String[] projection = {
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_MODIFIED
            };
            // Search for PDF files
            String selection = MediaStore.Files.FileColumns.MIME_TYPE + " = ?";
            String[] selectionArgs = new String[]{"application/pdf"};
            Uri queryUri = MediaStore.Files.getContentUri("external");

            try (Cursor cursor = getContentResolver().query(queryUri, projection, selection, selectionArgs, null)) {
                if (cursor != null) {
                    int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                    int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
                    int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
                    int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);

                    while (cursor.moveToNext()) {
                        String name = cursor.getString(nameColumn);
                        String path = cursor.getString(pathColumn);
                        long size = cursor.getLong(sizeColumn);
                        long date = cursor.getLong(dateColumn) * 1000; // Convert to milliseconds
                        fileList.add(new FileItem(name, path, size, date));
                    }
                }
            }
            
            // Sort by date, newest first
            Collections.sort(fileList, (f1, f2) -> Long.compare(f2.lastModified, f1.lastModified));

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                // Correctly create and set the adapter if it's the first time
                if (adapter == null) {
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    adapter = new FileListAdapter(this, fileList);
                    recyclerView.setAdapter(adapter);
                } else {
                    // Just notify the adapter that the data has changed
                    adapter.notifyDataSetChanged();
                }
            });
        }).start();
    }
}