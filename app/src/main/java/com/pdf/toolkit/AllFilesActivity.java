package com.pdf.toolkit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AllFilesActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout permissionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_files);

        recyclerView = findViewById(R.id.recycler_view_files);
        progressBar = findViewById(R.id.progress_bar);
        permissionView = findViewById(R.id.permission_needed_view);
        Button btnGrant = findViewById(R.id.btn_grant_permission);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        btnGrant.setOnClickListener(v -> requestStoragePermission());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasStoragePermission()) {
            permissionView.setVisibility(View.GONE);
            loadPDFFiles();
        } else {
            permissionView.setVisibility(View.VISIBLE);
        }
    }

    private void loadPDFFiles() {
        progressBar.setVisibility(View.VISIBLE);
        permissionView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        new Thread(() -> {
            List<FileItem> fileList = new ArrayList<>();
            File root = Environment.getExternalStorageDirectory();
            searchPDFFilesRecursively(root, fileList);

            Collections.sort(fileList, (a, b) -> Long.compare(b.date, a.date));

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (fileList.isEmpty()) {
                    Toast.makeText(this, "No PDF files found", Toast.LENGTH_SHORT).show();
                }
                // This uses your original, working adapter and click listener
                FileListAdapter adapter = new FileListAdapter(fileList, item -> {
                    Intent intent = new Intent(AllFilesActivity.this, PdfViewerActivity.class);
                    File file = new File(item.path);
                    Uri fileUri = Uri.fromFile(file);
                    intent.putExtra(PdfViewerActivity.EXTRA_FILE_URI, fileUri.toString());
                    startActivity(intent);
                });
                recyclerView.setAdapter(adapter);
            });
        }).start();
    }

    // --- THIS IS THE CORRECTED METHOD WITH THE TRASH FILTER ---
    private void searchPDFFilesRecursively(File dir, List<FileItem> fileList) {
        if (dir == null || !dir.isDirectory()) return;

        // Get the absolute path to check against trash folders
        String dirPath = dir.getAbsolutePath();

        // Skip common Android trash/data folders to avoid showing deleted or system files
        if (dirPath.contains("/.Trash") || dirPath.contains("/Android/data") || dirPath.contains("/.recycle")) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                searchPDFFilesRecursively(file, fileList);
            } else if (file.getName().toLowerCase().endsWith(".pdf")) {
                fileList.add(new FileItem(
                        file.getName(),
                        file.length(),
                        file.lastModified(),
                        file.getAbsolutePath()
                ));
            }
        }
    }

    // (The rest of your original, working code remains unchanged)
    private boolean hasStoragePermission() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { return Environment.isExternalStorageManager(); } else { return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED; } }
    private void requestStoragePermission() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION); intent.setData(Uri.parse("package:" + getPackageName())); startActivity(intent); } else { ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSIONS); } }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { super.onRequestPermissionsResult(requestCode, permissions, grantResults); if (requestCode == REQUEST_CODE_PERMISSIONS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { loadPDFFiles(); } else { Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show(); } }
    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
