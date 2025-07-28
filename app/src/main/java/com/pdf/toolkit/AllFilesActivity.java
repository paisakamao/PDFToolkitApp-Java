package com.pdf.toolkit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AllFilesActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout permissionView;
    private Button btnGrantPermission;

    private List<FileItem> fileList = new ArrayList<>();
    private FileListAdapter fileAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_files);

        recyclerView = findViewById(R.id.recycler_view_files);
        progressBar = findViewById(R.id.progress_bar);
        permissionView = findViewById(R.id.permission_needed_view);
        btnGrantPermission = findViewById(R.id.btn_grant_permission);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        fileAdapter = new FileListAdapter(fileList, this::onFileSelected);
        recyclerView.setAdapter(fileAdapter);

        btnGrantPermission.setOnClickListener(v -> checkAndLoadFiles());

        checkAndLoadFiles();
    }

    private void checkAndLoadFiles() {
        if (hasStoragePermission()) {
            permissionView.setVisibility(View.GONE);
            loadAllPdfFiles();
        } else {
            requestStoragePermission();
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                storagePermissionLauncher.launch(intent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error opening permission settings", Toast.LENGTH_SHORT).show();
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    private final ActivityResultLauncher<Intent> storagePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (hasStoragePermission()) {
                    checkAndLoadFiles();
                } else {
                    permissionView.setVisibility(View.VISIBLE);
                }
            }
    );

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkAndLoadFiles();
            } else {
                permissionView.setVisibility(View.VISIBLE);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void loadAllPdfFiles() {
        progressBar.setVisibility(View.VISIBLE);
        fileList.clear();

        File rootDir = Environment.getExternalStorageDirectory();
        scanForPdfRecursively(rootDir);

        fileAdapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
    }

    private void scanForPdfRecursively(File dir) {
        if (dir != null && dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        scanForPdfRecursively(file); // recursive call
                    } else if (file.getName().toLowerCase().endsWith(".pdf")) {
                        fileList.add(new FileItem(file.getName(), file.getAbsolutePath(), file.length(), file.lastModified()));
                    }
                }
            }
        }
    }

    private void onFileSelected(FileItem item) {
        if (item != null && item.path != null) {
            Intent intent = new Intent(this, PdfViewerActivity.class);
            intent.putExtra(PdfViewerActivity.EXTRA_FILE_NAME, item.path);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Error: No file specified", Toast.LENGTH_SHORT).show();
        }
    }
}