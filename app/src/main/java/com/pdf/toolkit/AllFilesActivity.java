package com.pdf.toolkit;

// All your existing, correct imports
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
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AllFilesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FileListAdapter adapter;
    private View permissionView;

    // --- START: IN-MEMORY CACHE ---
    // A 'static' variable belongs to the class, not the screen.
    // This means it will survive even when this screen is closed and re-opened.
    private static final List<FileItem> fileCache = new ArrayList<>();
    // --- END: IN-MEMORY CACHE ---

    // All your permission launchers are correct
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
        
        // --- START: IN-MEMORY CACHE LOGIC ---
        // We now initialize the adapter with the CACHE, not an empty list.
        adapter = new FileListAdapter(fileCache, this::openFileBasedOnType);
        recyclerView.setAdapter(adapter);
        
        // If the cache is NOT empty, we don't need to do anything else. The list will appear instantly.
        // If the cache IS empty, we need to check permissions and load the files.
        if (fileCache.isEmpty()) {
            checkPermissionAndLoadFiles();
        } else {
            // If we have cached files, make sure the correct UI is showing.
            showFileListUI();
        }
        // --- END: IN-MEMORY CACHE LOGIC ---
        
        grantPermissionButton.setOnClickListener(v -> checkPermissionAndLoadFiles());
    }
    
    // We no longer need the onResume() method, as the cache handles everything.

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
            
            String[] projection = { MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DATE_MODIFIED, MediaStore.Files.FileColumns.DATA };
            
            // --- START: SUPER-FAST SEARCH QUERY ---
            // This is a powerful filter that tells the database to only give us the files we want.
            // This is much faster than looping through all files in Java.
            String selection = MediaStore.Files.FileColumns.MIME_TYPE + " = ? OR " +
                               MediaStore.Files.FileColumns.MIME_TYPE + " = ? OR " +
                               MediaStore.Files.FileColumns.MIME_TYPE + " = ? OR " +
                               MediaStore.Files.FileColumns.MIME_TYPE + " = ?";
            String[] selectionArgs = new String[]{
                "application/pdf", 
                "application/msword", // .doc
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
                "text/plain" // .txt
            };
            // --- END: SUPER-FAST SEARCH QUERY ---

            String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC";

            try (Cursor cursor = getContentResolver().query(collection, projection, selection, selectionArgs, sortOrder)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        // We still do a "reality check" to avoid ghost files, but it's much faster now
                        // because we are only checking a small number of document files.
                        String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
                        if (path != null) {
                            File file = new File(path);
                            if (file.exists()) {
                                String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME));
                                long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE));
                                long date = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED));
                                realFiles.add(new FileItem(name, size, date * 1000));
                            }
                        }
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }

            runOnUiThread(() -> {
                Toast.makeText(AllFilesActivity.this, "Found " + realFiles.size() + " files.", Toast.LENGTH_LONG).show();
                
                // --- START: IN-MEMORY CACHE LOGIC ---
                // We update both the cache and the local list at the same time.
                fileCache.clear();
                fileCache.addAll(realFiles);
                adapter.notifyDataSetChanged();
                // --- END: IN-MEMORY CACHE LOGIC ---
            });
        }).start();
    }

    // --- The rest of your file (opening logic, data class, adapter) is already correct ---
    private void openFileBasedOnType(FileItem item){if(item.name!=null&&item.name.toLowerCase().endsWith(".pdf")){openPdfInApp(item);}else{openFileExternally(item);}}
    private void openPdfInApp(FileItem item){Intent i=new Intent(this,PdfViewerActivity.class);i.putExtra(PdfViewerActivity.EXTRA_FILE_NAME,item.name);startActivity(i);}
    private void openFileExternally(FileItem item){File d=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);File f=new File(d,item.name);if(!f.exists()){Toast.makeText(this,"Error: File not found.",Toast.LENGTH_SHORT).show();return;}
    Uri u=FileProvider.getUriForFile(this,"com.pdf.toolkit.fileprovider",f);Intent i=new Intent(Intent.ACTION_VIEW);String m=getMimeType(item.name);i.setDataAndType(u,m);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivity(i);}catch(ActivityNotFoundException e){Toast.makeText(this,"No app found to open this file type.",Toast.LENGTH_LONG).show();}}
    private String getMimeType(String n){String e=MimeTypeMap.getFileExtensionFromUrl(n);if(e!=null){String m=MimeTypeMap.getSingleton().getMimeTypeFromExtension(e.toLowerCase());if(m!=null)return m;}return"application/octet-stream";}
    private void showFileListUI(){recyclerView.setVisibility(View.VISIBLE);permissionView.setVisibility(View.GONE);}
    private void showPermissionNeededUI(){recyclerView.setVisibility(View.GONE);permissionView.setVisibility(View.VISIBLE);}
    public static class FileItem { String name; long size; long date; public FileItem(String name, long size, long date) { this.name = name; this.size = size; this.date = date; }}
}