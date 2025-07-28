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
import android.widget.ProgressBar;
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
    private final List<FileItem> fileList = new ArrayList<>();
    private View permissionView;
    private ProgressBar progressBar;

    private final ActivityResultLauncher<Intent> requestAllFilesAccessLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> checkPermissionAndLoadFiles());
    private final ActivityResultLauncher<String> requestLegacyPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), g -> { if (g) loadFilesFromStorage(); else showPermissionNeededUI(); });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_files);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("All Files");
        }

        recyclerView = findViewById(R.id.recycler_view_files);
        permissionView = findViewById(R.id.permission_needed_view);
        progressBar = findViewById(R.id.progress_bar);
        Button grantPermissionButton = findViewById(R.id.btn_grant_permission);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileListAdapter(fileList, this::openFileBasedOnType);
        recyclerView.setAdapter(adapter);
        
        grantPermissionButton.setOnClickListener(v -> checkPermissionAndLoadFiles());
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionAndLoadFiles();
    }

    private void checkPermissionAndLoadFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                loadFilesFromStorage();
            } else {
                showPermissionNeededUI();
                if (fileList.isEmpty()) {
                    Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri u = Uri.fromParts("package", getPackageName(), null);
                    i.setData(u);
                    requestAllFilesAccessLauncher.launch(i);
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                loadFilesFromStorage();
            } else {
                showPermissionNeededUI();
                if (fileList.isEmpty()) {
                    requestLegacyPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
            }
        }
    }

    private void loadFilesFromStorage() {
        showLoadingUI();
        new Thread(() -> {
            final List<FileItem> realFiles = new ArrayList<>();
            Uri collection = MediaStore.Files.getContentUri("external");
            
            String[] projection = { MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DATE_MODIFIED, MediaStore.Files.FileColumns.DATA };
            
            List<String> selectionArgsList = new ArrayList<>();
            StringBuilder selection = new StringBuilder();
            String[] mimeTypes = {"application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain"};
            selection.append(MediaStore.Files.FileColumns.MIME_TYPE + " IN (");
            for (int i = 0; i < mimeTypes.length; i++) {
                selection.append("?,");
                selectionArgsList.add(mimeTypes[i]);
            }
            selection.deleteCharAt(selection.length() - 1);
            selection.append(")");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                selection.append(" AND " + MediaStore.MediaColumns.IS_TRASHED + " = 0");
            }
            selection.append(" AND " + MediaStore.Files.FileColumns.DATA + " NOT LIKE ?");
            selectionArgsList.add("%/.%");
            selection.append(" AND " + MediaStore.Files.FileColumns.DATA + " NOT LIKE ?");
            selectionArgsList.add("%/Android/data/%");

            String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC";
            String[] selectionArgs = selectionArgsList.toArray(new String[0]);

            try (Cursor cursor = getContentResolver().query(collection, projection, selection.toString(), selectionArgs, sortOrder)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME));
                        long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE));
                        long date = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED));
                        String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
                        realFiles.add(new FileItem(name, size, date * 1000, path));
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }

            runOnUiThread(() -> {
                showFileListUI();
                fileList.clear();
                fileList.addAll(realFiles);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void openFileBasedOnType(FileItem item){if(item.name!=null&&item.name.toLowerCase().endsWith(".pdf")){openPdfInApp(item);}else{openFileExternally(item);}}
    private void openPdfInApp(FileItem item){Intent i=new Intent(this,PdfViewerActivity.class);i.putExtra(PdfViewerActivity.EXTRA_FILE_NAME,item.path);startActivity(i);}
    private void openFileExternally(FileItem item){File f=new File(item.path);if(!f.exists()){Toast.makeText(this,"Error: File no longer exists.",Toast.LENGTH_SHORT).show();return;}
    Uri u=FileProvider.getUriForFile(this,"com.pdf.toolkit.fileprovider",f);Intent i=new Intent(Intent.ACTION_VIEW);String m=getMimeType(item.name);i.setDataAndType(u,m);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivity(i);}catch(ActivityNotFoundException e){Toast.makeText(this,"No app found to open this file type.",Toast.LENGTH_LONG).show();}}
    private String getMimeType(String n){String e=MimeTypeMap.getFileExtensionFromUrl(n);if(e!=null){String m=MimeTypeMap.getSingleton().getMimeTypeFromExtension(e.toLowerCase());if(m!=null)return m;}return"application/octet-stream";}
    private void showLoadingUI(){progressBar.setVisibility(View.VISIBLE);recyclerView.setVisibility(View.GONE);permissionView.setVisibility(View.GONE);}
    private void showFileListUI(){progressBar.setVisibility(View.GONE);recyclerView.setVisibility(View.VISIBLE);permissionView.setVisibility(View.GONE);}
    private void showPermissionNeededUI(){progressBar.setVisibility(View.GONE);recyclerView.setVisibility(View.GONE);permissionView.setVisibility(View.VISIBLE);}
}
