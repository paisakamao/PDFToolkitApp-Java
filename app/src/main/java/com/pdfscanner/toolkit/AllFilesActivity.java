package com.pdfscanner.toolkit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AllFilesActivity extends AppCompatActivity implements FileListAdapter.OnFileClickListener {

    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private static final int FIRST_AD_POSITION = 3; // Show first ad at 3rd item
    private static final int AD_INTERVAL = 7;       // Then after every 7 items

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout permissionView;
    private TextView emptyView;

    private FileListAdapter adapter;
    private List<Object> itemList = new ArrayList<>(); // Changed to Object for mixing ads + files
    private List<FileItem> fileList = new ArrayList<>();

    private ActionMode actionMode;
    private Toolbar toolbar; // Keep a reference to the toolbar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_files);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("All Files");
        setSupportActionBar(toolbar);
        toolbar.setTitleTextAppearance(this, R.style.ToolbarTitle_Small);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        recyclerView = findViewById(R.id.recycler_view_files);
        progressBar = findViewById(R.id.progress_bar);
        permissionView = findViewById(R.id.permission_needed_view);
        emptyView = findViewById(R.id.empty_view_text);
        Button btnGrant = findViewById(R.id.btn_grant_permission);
        btnGrant.setOnClickListener(v -> requestStoragePermission());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileListAdapter(new ArrayList<>(), this); // normal adapter
        recyclerView.setAdapter(adapter);
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

    // 🔹 Load Native Ads
    private void loadNativeAds() {
        String nativeAdId = FirebaseRemoteConfig.getInstance()
                .getString("admob_native_ad_unit_id");

        AdLoader adLoader = new AdLoader.Builder(this, nativeAdId)
                .forNativeAd(nativeAd -> {
                    insertAdIntoList(nativeAd);
                    adapter.notifyDataSetChanged();
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull com.google.android.gms.ads.LoadAdError adError) {
                        Toast.makeText(AllFilesActivity.this, "Ad failed: " + adError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .build();

        for (int i = 0; i < 3; i++) {
            adLoader.loadAd(new AdRequest.Builder().build());
        }
    }

    private void insertAdIntoList(NativeAd nativeAd) {
        int nextAdPosition = FIRST_AD_POSITION;
        while (nextAdPosition < itemList.size()) {
            if (!(itemList.get(nextAdPosition) instanceof NativeAd)) {
                itemList.add(nextAdPosition, nativeAd);
                break;
            }
            nextAdPosition += AD_INTERVAL;
        }
    }

    @Override
    public void onFileClick(FileItem item) {
        if (actionMode != null) {
            toggleSelection(item);
        } else {
            Intent intent = new Intent(AllFilesActivity.this, PdfViewerActivity.class);
            File file = new File(item.path);
            Uri fileUri = Uri.fromFile(file);
            intent.putExtra(PdfViewerActivity.EXTRA_FILE_URI, fileUri.toString());
            startActivity(intent);
        }
    }

    @Override
    public void onFileLongClick(FileItem item) {
        if (actionMode == null) {
            actionMode = toolbar.startActionMode(actionModeCallback);
        }
        toggleSelection(item);
    }

    private void toggleSelection(FileItem item) {
        adapter.toggleSelection(item);
        int count = adapter.getSelectedItemCount();
        if (count == 0 && actionMode != null) {
            actionMode.finish();
        } else if (actionMode != null) {
            actionMode.setTitle(count + " selected");
        }
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_contextual, menu);
            adapter.setMultiSelectMode(true);
            return true;
        }

        @Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
        @Override public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.action_delete) {
                deleteSelectedFiles();
                return true;
            } else if (itemId == R.id.action_share) {
                shareSelectedFiles();
                return true;
            }
            return false;
        }
        @Override public void onDestroyActionMode(ActionMode mode) {
            adapter.setMultiSelectMode(false);
            actionMode = null;
        }
    };

    private void deleteSelectedFiles() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Files")
                .setMessage("Are you sure you want to delete " + adapter.getSelectedItemCount() + " file(s)?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    for (FileItem item : adapter.getSelectedItems()) {
                        new File(item.path).delete();
                    }
                    Toast.makeText(this, "Files deleted", Toast.LENGTH_SHORT).show();
                    actionMode.finish();
                    loadPDFFiles();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void shareSelectedFiles() {
        ArrayList<Uri> uris = new ArrayList<>();
        String authority = getApplicationContext().getPackageName() + ".provider";
        for (FileItem item : adapter.getSelectedItems()) {
            File file = new File(item.path);
            uris.add(FileProvider.getUriForFile(this, authority, file));
        }
        if (uris.isEmpty()) return;
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (uris.size() > 1) {
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            intent.setType("application/pdf");
        } else {
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
            intent.setType("application/pdf");
        }
        startActivity(Intent.createChooser(intent, "Share PDF(s)"));
        actionMode.finish();
    }

    // 🔹 Load PDFs
    private void loadPDFFiles() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        new Thread(() -> {
            List<FileItem> freshFileList = new ArrayList<>();
            File root = Environment.getExternalStorageDirectory();
            searchPDFFilesRecursively(root, freshFileList);
            Collections.sort(freshFileList, (a, b) -> Long.compare(b.date, a.date));

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                fileList.clear();
                fileList.addAll(freshFileList);

                itemList.clear();
                itemList.addAll(fileList); // files first
                adapter.notifyDataSetChanged();

                if (fileList.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                    loadNativeAds(); // 🔹 Load ads after files loaded
                }
            });
        }).start();
    }

    private void searchPDFFilesRecursively(File dir, List<FileItem> fileList) {
        if (dir == null || !dir.isDirectory()) return;
        String dirPath = dir.getAbsolutePath();
        if (dirPath.contains("/.Trash") || dirPath.contains("/Android/data") || dirPath.contains("/.recycle")) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                searchPDFFilesRecursively(file, fileList);
            } else if (file.getName().toLowerCase().endsWith(".pdf")) {
                fileList.add(new FileItem(file.getName(), file.length(), file.lastModified(), file.getAbsolutePath()));
            }
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
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadPDFFiles();
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
package com.pdfscanner.toolkit;

import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.NativeAd;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AllFilesActivity extends AppCompatActivity implements FileListAdapter.OnFileClickListener {

    private RecyclerView recyclerView;
    private FileListAdapter adapter;
    private final List<Object> itemList = new ArrayList<>(); // Can hold FileItem or NativeAd
    private boolean isMultiSelectMode = false;

    private static final int FIRST_AD_POSITION = 3;
    private static final int AD_INTERVAL = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_files);

        recyclerView = findViewById(R.id.recycler_view_files);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));



        loadFiles();
        loadNativeAds();

        adapter = new FileListAdapter(this, itemList, this);
        recyclerView.setAdapter(adapter);
    }

    private void loadFiles() {
        itemList.clear();

        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));

        if (files != null) {
            for (File file : files) {
                itemList.add(new FileItem(file.getName(), file.length(), file.lastModified(), file.getAbsolutePath()));
            }
        }
    }

private void loadNativeAds() {
    String nativeAdId = FirebaseRemoteConfig.getInstance()
            .getString("admob_native_ad_unit_id");

    AdLoader adLoader = new AdLoader.Builder(this, nativeAdId)
            .forNativeAd(nativeAd -> {
                insertAdIntoList(nativeAd);
                adapter.notifyDataSetChanged();
            })
            .withAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(@NonNull com.google.android.gms.ads.LoadAdError adError) {
                    Toast.makeText(AllFilesActivity.this, "Ad failed: " + adError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            })
            .build();

    // Request multiple ads
    for (int i = 0; i < 3; i++) {
        adLoader.loadAd(new AdRequest.Builder().build());
    }
}


private void insertAdIntoList(NativeAd nativeAd) {
    int nextAdPosition = FIRST_AD_POSITION;
    while (nextAdPosition < itemList.size()) {
        if (!(itemList.get(nextAdPosition) instanceof NativeAd)) {
            itemList.add(nextAdPosition, nativeAd);
            break;
        }
        nextAdPosition += AD_INTERVAL;
    }
}


    // ================= Adapter Callbacks =================
    @Override
    public void onFileClick(FileItem item) {
        if (isMultiSelectMode) {
            adapter.toggleSelection(item);
        } else {
            Toast.makeText(this, "Clicked: " + item.name, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFileLongClick(FileItem item) {
        if (!isMultiSelectMode) {
            isMultiSelectMode = true;
            adapter.setMultiSelectMode(true);
            adapter.toggleSelection(item);
            invalidateOptionsMenu();
        }
    }

    // ================= Action Bar =================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_contextual, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_delete).setVisible(isMultiSelectMode);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            deleteSelectedFiles();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteSelectedFiles() {
        List<FileItem> selected = adapter.getSelectedItems();
        for (FileItem fileItem : selected) {
            File file = new File(fileItem.path);
            if (file.exists()) {
                file.delete();
            }
            itemList.remove(fileItem);
        }
        adapter.clearSelections();
        isMultiSelectMode = false;
        invalidateOptionsMenu();
        adapter.notifyDataSetChanged();
    }
}
