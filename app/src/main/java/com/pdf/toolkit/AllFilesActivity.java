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

    // All your permission launchers remain the same
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
        
        adapter = new FileListAdapter(fileList, fileItem -> {
            if (fileItem.name != null && fileItem.name.toLowerCase().endsWith(".pdf")) {
                openPdfInApp(fileItem);
            } else {
                openFileExternally(fileItem);
            }
        });
        recyclerView.setAdapter(adapter);
        
        grantPermissionButton.setOnClickListener(v -> checkPermissionAndLoadFiles());
    }

    private void openPdfInApp(FileItem item) {
        Intent intent = new Intent(this, PdfViewerActivity.class);
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
    
    private String getMimeType(String fileName) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
        if (extension != null) {
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (mime != null) return mime;
        }
        return "application/octet-stream";
    }

    // The rest of your code remains the same...
    @Override protected void onResume(){super.onResume();checkPermissionAndLoadFiles();}
    private void checkPermissionAndLoadFiles(){if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){if(Environment.isExternalStorageManager()){showFileListUI();loadFilesFromStorage();}else{showPermissionNeededUI();Intent i=new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);Uri u=Uri.fromParts("package",getPackageName(),null);i.setData(u);requestAllFilesAccessLauncher.launch(i);}}else{if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){showFileListUI();loadFilesFromStorage();}else{showPermissionNeededUI();requestLegacyPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);}}}
    private void loadFilesFromStorage(){new Thread(()->{fileList.clear();Uri u=MediaStore.Files.getContentUri("external");String[]p={MediaStore.Files.FileColumns.DISPLAY_NAME,MediaStore.Files.FileColumns.SIZE,MediaStore.Files.FileColumns.DATE_MODIFIED};String s=MediaStore.Files.FileColumns.DATE_MODIFIED+" DESC";try(Cursor c=getContentResolver().query(u,p,null,null,s)){if(c!=null){int nc=c.getColumnIndexOrThrow(p[0]);int sc=c.getColumnIndexOrThrow(p[1]);int dc=c.getColumnIndexOrThrow(p[2]);while(c.moveToNext()){String n=c.getString(nc);if(n!=null&&(n.toLowerCase().endsWith(".pdf")||n.toLowerCase().endsWith(".doc")||n.toLowerCase().endsWith(".docx")||n.toLowerCase().endsWith(".txt"))){long sz=c.getLong(sc);long dt=c.getLong(dc);fileList.add(new FileItem(n,sz,dt*1000));}}}}
    runOnUiThread(()->adapter.notifyDataSetChanged());}).start();}
    private void showFileListUI(){recyclerView.setVisibility(View.VISIBLE);permissionView.setVisibility(View.GONE);}
    private void showPermissionNeededUI(){recyclerView.setVisibility(View.GONE);permissionView.setVisibility(View.VISIBLE);}
    public static class FileItem{String name;long size;long date;public FileItem(String n,long s,long d){name=n;size=s;date=d;}}
    
    public static class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> {
        private final List<FileItem> files;
        private final OnFileClickListener listener;
        public interface OnFileClickListener { void onFileClick(FileItem item); }
        public FileListAdapter(List<FileItem> files, OnFileClickListener listener){this.files=files;this.listener=listener;}
        @Override public FileViewHolder onCreateViewHolder(ViewGroup parent,int viewType){View v=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file,parent,false);return new FileViewHolder(v);}
        @Override public void onBindViewHolder(FileViewHolder h,int p){FileItem f=files.get(p);h.bind(f, listener);}
        @Override public int getItemCount(){return files.size();}
        
        // --- START: THIS IS THE FIX ---
        // These helper methods are now 'static', so they can be called from the static FileViewHolder.
        private static String formatDate(long millis) {
            return new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(new Date(millis));
        }

        private static String formatFileSize(long size) {
            if (size < 1024) return size + " B";
            int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
            return String.format(Locale.US, "%.1f %sB", (double) size / (1L << (z * 10)), " KMGTPE".charAt(z));
        }
        // --- END: THIS IS THE FIX ---

        public static class FileViewHolder extends RecyclerView.ViewHolder {
            ImageView fileIcon; TextView fileName; TextView fileDetails;
            public FileViewHolder(View itemView) {
                super(itemView);
                fileIcon = itemView.findViewById(R.id.icon_file_type);
                fileName = itemView.findViewById(R.id.text_file_name);
                fileDetails = itemView.findViewById(R.id.text_file_details);
            }

            public void bind(final FileItem item, final OnFileClickListener listener) {
                fileName.setText(item.name);
                // The calls are now correct because the methods they are calling are also static
                fileDetails.setText(FileListAdapter.formatFileSize(item.size) + " - " + FileListAdapter.formatDate(item.date));
                if (item.name.toLowerCase().endsWith(".pdf")) {
                    fileIcon.setImageResource(android.R.drawable.ic_menu_gallery);
                } else {
                    fileIcon.setImageResource(android.R.drawable.ic_menu_edit);
                }
                itemView.setOnClickListener(v -> listener.onFileClick(item));
            }
        }
    }
}
