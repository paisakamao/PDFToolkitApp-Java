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

    // All your permission launchers are correct and remain the same
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
        
        adapter = new FileListAdapter(fileList, this::openFileBasedOnType);
        recyclerView.setAdapter(adapter);
        
        grantPermissionButton.setOnClickListener(v -> checkPermissionAndLoadFiles());
    }

    // --- START: THIS IS THE CORRECTED AND FINAL VERSION ---
    private void loadFilesFromStorage() {
        new Thread(() -> {
            // This is a temporary list to hold the files we find.
            final List<FileItem> foundFiles = new ArrayList<>();
            
            Uri collection;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
            } else {
                collection = MediaStore.Files.getContentUri("external");
            }

            String[] projection = {
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_MODIFIED
            };
            
            // This is a more robust way to select only the file types you want.
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

            String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC";

            try (Cursor cursor = getContentResolver().query(collection, projection, selection, selectionArgs, sortOrder)) {
                if (cursor != null) {
                    int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                    int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
                    int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);

                    while (cursor.moveToNext()) {
                       String name = cursor.getString(nameColumn);
                       long size = cursor.getLong(sizeColumn);
                       long date = cursor.getLong(dateColumn);
                       foundFiles.add(new FileItem(name, size, date * 1000));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Update the UI on the main thread
            runOnUiThread(() -> {
                // --- THIS IS THE CRITICAL DEBUGGING STEP ---
                Toast.makeText(AllFilesActivity.this, "Found " + foundFiles.size() + " files.", Toast.LENGTH_LONG).show();
                
                fileList.clear();
                fileList.addAll(foundFiles);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }
    // --- END: THIS IS THE CORRECTED AND FINAL VERSION ---

    // The rest of your AllFilesActivity.java code is already correct and does not need to be changed.
    @Override protected void onResume(){super.onResume();checkPermissionAndLoadFiles();}
    private void checkPermissionAndLoadFiles(){if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){if(Environment.isExternalStorageManager()){showFileListUI();loadFilesFromStorage();}else{showPermissionNeededUI();Intent i=new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);Uri u=Uri.fromParts("package",getPackageName(),null);i.setData(u);requestAllFilesAccessLauncher.launch(i);}}else{if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){showFileListUI();loadFilesFromStorage();}else{showPermissionNeededUI();requestLegacyPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);}}}
    private void openFileBasedOnType(FileItem item){if(item.name!=null&&item.name.toLowerCase().endsWith(".pdf")){openPdfInApp(item);}else{openFileExternally(item);}}
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