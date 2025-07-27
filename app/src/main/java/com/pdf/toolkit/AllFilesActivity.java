package com.pdf.toolkit;

// All your existing, correct imports are here...
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
    private static final List<FileItem> fileCache = new ArrayList<>();

    private final ActivityResultLauncher<Intent> requestAllFilesAccessLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> checkPermissionAndLoadFiles());
    private final ActivityResultLauncher<String> requestLegacyPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), g -> { if (g) loadFilesFromStorage(); else showPermissionNeededUI(); });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_files);

        recyclerView = findViewById(R.id.recycler_view_files);
        permissionView = findViewById(R.id.permission_needed_view);
        Button grantPermissionButton = findViewById(R.id.btn_grant_permission);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileListAdapter(fileCache, this::openFileBasedOnType);
        recyclerView.setAdapter(adapter);
        
        if (fileCache.isEmpty()) {
            checkPermissionAndLoadFiles();
        } else {
            showFileListUI();
        }
        
        grantPermissionButton.setOnClickListener(v -> checkPermissionAndLoadFiles());
    }
    
    private void checkPermissionAndLoadFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                showFileListUI();
                loadFilesFromStorage();
            } else {
                showPermissionNeededUI();
                Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri u = Uri.fromParts("package", getPackageName(), null);
                i.setData(u);
                requestAllFilesAccessLauncher.launch(i);
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
        Toast.makeText(this, "Searching for files...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            final List<FileItem> realFiles = new ArrayList<>();
            Uri collection = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ?
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL) : MediaStore.Files.getContentUri("external");
            
            // --- THE CRITICAL CHANGE IS HERE: WE ASK FOR THE FULL PATH ---
            String[] projection = {
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_MODIFIED,
                    MediaStore.Files.FileColumns.DATA // This gets the full file path
            };
            
            String selection = MediaStore.Files.FileColumns.MIME_TYPE + " = ? OR " +
                               MediaStore.Files.FileColumns.MIME_TYPE + " = ? OR " +
                               MediaStore.Files.FileColumns.MIME_TYPE + " = ? OR " +
                               MediaStore.Files.FileColumns.MIME_TYPE + " = ?";
            String[] selectionArgs = new String[]{ "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain" };
            String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC";

            try (Cursor cursor = getContentResolver().query(collection, projection, selection, selectionArgs, sortOrder)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME));
                        long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE));
                        long date = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED));
                        String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)); // We get the path
                        
                        // We add the path to our FileItem
                        realFiles.add(new FileItem(name, size, date * 1000, path));
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }

            runOnUiThread(() -> {
                Toast.makeText(AllFilesActivity.this, "Found " + realFiles.size() + " files.", Toast.LENGTH_LONG).show();
                fileCache.clear();
                fileCache.addAll(realFiles);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void openFileBasedOnType(FileItem item) {
        // --- THE CRITICAL CHANGE IS HERE: WE USE THE SAVED PATH ---
        File file = new File(item.path);
        
        if (!file.exists()) {
            Toast.makeText(this, "Error: File no longer exists.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (item.name != null && item.name.toLowerCase().endsWith(".pdf")) {
            openPdfInApp(item);
        } else {
            openFileExternally(item);
        }
    }

    private void openPdfInApp(FileItem item) {
        Intent intent = new Intent(this, PdfViewerActivity.class);
        intent.putExtra(PdfViewerActivity.EXTRA_FILE_NAME, item.path); // We send the full path
        startActivity(intent);
    }

    private void openFileExternally(FileItem item) {
        File file = new File(item.path); // Use the correct path
        Uri uri = FileProvider.getUriForFile(this, "com.pdf.toolkit.fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String mimeType = getMimeType(item.name);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app found to open this file type.", Toast.LENGTH_LONG).show();
        }
    }

    private String getMimeType(String n){String e=MimeTypeMap.getFileExtensionFromUrl(n);if(e!=null){String m=MimeTypeMap.getSingleton().getMimeTypeFromExtension(e.toLowerCase());if(m!=null)return m;}return"application/octet-stream";}
    private void showFileListUI(){recyclerView.setVisibility(View.VISIBLE);permissionView.setVisibility(View.GONE);}
    private void showPermissionNeededUI(){recyclerView.setVisibility(View.GONE);permissionView.setVisibility(View.VISIBLE);}
    
    // --- THE CRITICAL CHANGE IS HERE: The FileItem now stores the path ---
    public static class FileItem {
        String name;
        long size;
        long date;
        String path; // The full path to the file
        public FileItem(String name, long size, long date, String path) {
            this.name = name;
            this.size = size;
            this.date = date;
            this.path = path;
        }
    }
}