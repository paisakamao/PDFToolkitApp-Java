package com.pdf.toolkit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

public class AllFilesActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_REQUEST = 100;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View permissionView;
    private TextView titleText;

    private FileAdapter fileAdapter;
    private ArrayList<FileItem> fileList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_files);

        recyclerView = findViewById(R.id.recycler_view_files);
        progressBar = findViewById(R.id.progress_bar);
        permissionView = findViewById(R.id.permission_needed_view);
        titleText = findViewById(R.id.textViewAllFilesTitle);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fileAdapter = new FileAdapter(fileList, this::onFileSelected);
        recyclerView.setAdapter(fileAdapter);

        checkStoragePermission();
    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST);
        } else {
            loadFiles();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFiles();
            } else {
                permissionView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadFiles() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        permissionView.setVisibility(View.GONE);

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File[] files = downloadsDir.listFiles();

        fileList.clear();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".pdf")) {
                    fileList.add(new FileItem(file.getName(), file.length(), file.lastModified(), file.getAbsolutePath()));
                }
            }
        }

        fileAdapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void onFileSelected(FileItem item) {
        if (item != null && item.path != null) {
            Intent intent = new Intent(AllFilesActivity.this, PdfViewerActivity.class);
            intent.putExtra(PdfViewerActivity.EXTRA_FILE_NAME, item.path);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Error: No file specified", Toast.LENGTH_SHORT).show();
        }
    }
}