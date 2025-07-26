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

import java.io.File; // This import is now essential
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

    // ... (Your permission launchers remain the same)
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
        adapter = new FileListAdapter(fileList, this::openFileBasedOnType); // Simplified click handler
        recyclerView.setAdapter(adapter);
        
        grantPermissionButton.setOnClickListener(v -> checkPermissionAndLoadFiles());
    }

    // A single, cleaner function to handle any file click
    private void openFileBasedOnType(FileItem item) {
        if (item.name != null && item.name.toLowerCase().endsWith(".pdf")) {
            openPdfInApp(item);
        } else {
            openFileExternally(item);
        }
    }
    
    // --- START: THIS IS THE CORRECTED FUNCTION ---
    private void loadFilesFromStorage() {
        new Thread(() -> {
            final List<FileItem> realFiles = new ArrayList<>();
            Uri uri = MediaStore.Files.getContentUri("external");
            String[] projection = {
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_MODIFIED,
                    MediaStore.Files.FileColumns.DATA // This is the key: the actual file path
            };
            String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC";

            try (Cursor cursor = getContentResolver().query(uri, projection, null, null, sortOrder)) {
                if (cursor != null) {
                    int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                    int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
                    int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
                    int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA); // Get the path column

                    while (cursor.moveToNext()) {
                        String path = cursor.getString(dataColumn);
                        // The "Reality Check": Create a file object from the path and see if it actually exists.
                        if (path != null) {
                            File file = new File(path);
                            if (file.exists()) {
                                // If the file is real, get its details and add it to our temporary list.
                                String name = cursor.getString(nameColumn);
                                if (name != null && (name.toLowerCase().endsWith(".pdf") || name.toLowerCase().endsWith(".doc") || name.toLowerCase().endsWith(".docx") || name.toLowerCase().endsWith(".txt"))) {
                                    long size = cursor.getLong(sizeColumn);
                                    long date = cursor.getLong(dateColumn);
                                    realFiles.add(new FileItem(name, size, date * 1000));
                                }
                            }
                        }
                    }
                }
            }
            // Update the UI on the main thread
            runOnUiThread(() -> {
                fileList.clear();
                fileList.addAll(realFiles); // Add only the real files
                adapter.notifyDataSetChanged();
            });
        }).start();
    }
    // --- END: THIS IS THE CORRECTED FUNCTION ---

    // The rest of your AllFilesActivity.java code is already correct and does not need to be changed.
    @Override protected void onResume(){super.onResume();checkPermissionAndLoadFiles();}
    private void checkPermissionAndLoadFiles(){if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){if(Environment.isExternalStorageManager()){showFileListUI();loadFilesFromStorage();}else{showPermissionNeededUI();Intent i=new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);Uri u=Uri.fromParts("package",getPackageName(),null);i.setData(u);requestAllFilesAccessLauncher.launch(i);}}else{if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){showFileListUI();loadFilesFromStorage();}else{showPermissionNeededUI();requestLegacyPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);}}}
    private void openPdfInApp(FileItem item){Intent i=new Intent(this,PdfViewerActivity.class);i.putExtra(PdfViewerActivity.EXTRA_FILE_NAME,item.name);startActivity(i);}
    private void openFileExternally(FileItem item){File d=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);File f=new File(d,item.name);if(!f.exists()){Toast.makeText(this,"Error: File not found.",Toast.LENGTH_SHORT).show();return;}
    Uri u=FileProvider.getUriForFile(this,"com.pdf.toolkit.fileprovider",f);Intent i=new Intent(Intent.ACTION_VIEW);String m=getMimeType(item.name);i.setDataAndType(u,m);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivity(i);}catch(ActivityNotFoundException e){Toast.makeText(this,"No app found to open this file type.",Toast.LENGTH_LONG).show();}}
    private String getMimeType(String n){String e=MimeTypeMap.getFileExtensionFromUrl(n);if(e!=null){String m=MimeTypeMap.getSingleton().getMimeTypeFromExtension(e.toLowerCase());if(m!=null)return m;}return"application/octet-stream";}
    private void showFileListUI(){recyclerView.setVisibility(View.VISIBLE);permissionView.setVisibility(View.GONE);}
    private void showPermissionNeededUI(){recyclerView.setVisibility(View.GONE);permissionView.setVisibility(View.VISIBLE);}
    public static class FileItem{String name;long size;long date;public FileItem(String n,long s,long d){name=n;size=s;date=d;}}
    public static class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder>{private final List<FileItem>files;private final OnFileClickListener listener;public interface OnFileClickListener{void onFileClick(FileItem item);}
    public FileListAdapter(List<FileItem>files,OnFileClickListener listener){this.files=files;this.listener=listener;}
    @Override public FileViewHolder onCreateViewHolder(ViewGroup p,int v){View i=LayoutInflater.from(p.getContext()).inflate(R.layout.item_file,p,false);return new FileViewHolder(i);}
    @Override public void onBindViewHolder(FileViewHolder h,int p){FileItem f=files.get(p);h.bind(f,listener);}
    @Override public int getItemCount(){return files.size();}
    public static class FileViewHolder extends RecyclerView.ViewHolder{ImageView i;TextView n;TextView d;public FileViewHolder(View v){super(v);i=v.findViewById(R.id.icon_file_type);n=v.findViewById(R.id.text_file_name);d=v.findViewById(R.id.text_file_details);}
    public void bind(final FileItem item,final OnFileClickListener listener){String s=formatFileSize(item.size)+" - "+formatDate(item.date);n.setText(item.name);d.setText(s);if(item.name.toLowerCase().endsWith(".pdf")){i.setImageResource(android.R.drawable.ic_menu_gallery);}else{i.setImageResource(android.R.drawable.ic_menu_edit);}itemView.setOnClickListener(v->listener.onFileClick(item));}
    private static String formatDate(long ms){return new SimpleDateFormat("MM/dd/yyyy",Locale.getDefault()).format(new Date(ms));}
    private static String formatFileSize(long s){if(s<1024)return s+" B";int z=(63-Long.numberOfLeadingZeros(s))/10;return String.format(Locale.US,"%.1f %sB",(double)s/(1L<<(z*10))," KMGTPE".charAt(z));}}}
}