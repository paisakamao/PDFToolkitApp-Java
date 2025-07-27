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
    
    // --- START: NEW "LOAD ONCE" LOGIC ---
    private boolean hasLoadedFiles = false;
    // --- END: NEW "LOAD ONCE" LOGIC ---

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
    
    @Override
    protected void onResume() {
        super.onResume();
        // --- START: NEW "LOAD ONCE" LOGIC ---
        // Only check permissions and load files if we haven't already done so.
        if (!hasLoadedFiles) {
            checkPermissionAndLoadFiles();
        }
        // --- END: NEW "LOAD ONCE" LOGIC ---
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

    // --- START: THIS IS THE FINAL, CORRECTED VERSION WITH THE "REALITY CHECK" ---
    private void loadFilesFromStorage() {
        new Thread(() -> {
            final List<FileItem> realFiles = new ArrayList<>();
            
            Uri collection;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
            } else {
                collection = MediaStore.Files.getContentUri("external");
            }

            String[] projection = {
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_MODIFIED,
                    MediaStore.Files.FileColumns.DATA // This is the key: the actual file path
            };
            
            String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC";

            try (Cursor cursor = getContentResolver().query(collection, projection, null, null, sortOrder)) {
                if (cursor != null) {
                    int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                    int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
                    int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
                    int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);

                    while (cursor.moveToNext()) {
                        String path = cursor.getString(dataColumn);
                        // The "Reality Check": This time it will work because we are checking the path.
                        if (path != null) {
                            File file = new File(path);
                            if (file.exists()) { // This is the critical check for "ghost files"
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
            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                // --- START: NEW "LOAD ONCE" LOGIC ---
                // We show the toast only the very first time.
                if (!hasLoadedFiles) {
                    Toast.makeText(AllFilesActivity.this, "Found " + realFiles.size() + " real files.", Toast.LENGTH_LONG).show();
                }
                hasLoadedFiles = true; // Mark that we have loaded the files.
                // --- END: NEW "LOAD ONCE" LOGIC ---
                
                fileList.clear();
                fileList.addAll(realFiles);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }
    // --- END: THIS IS THE FINAL, CORRECTED VERSION ---

    // The rest of your AllFilesActivity.java code is already correct.
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