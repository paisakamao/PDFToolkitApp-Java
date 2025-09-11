// File Location: app/src/main/java/com/pdfscanner/toolkit/AllFilesActivity.java
package com.pdfscanner.toolkit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
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
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AllFilesActivity extends AppCompatActivity implements FileListAdapter.OnFileInteractionListener {

    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private static final String TAG = "AllFilesActivity";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout permissionView;
    private TextView emptyView;
    private Toolbar toolbar;
    private ActionMode actionMode;

    private FileListAdapter adapter;
    private final List<Object> combinedList = new ArrayList<>();

    private final Map<Integer, NativeAd> loadedAds = new HashMap<>();
    private final Set<Integer> positionsCurrentlyLoading = new HashSet<>();
    private static final int FIRST_AD_POSITION = 3;
    private static final int AD_INTERVAL = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_files);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("All Files");
        setSupportActionBar(toolbar);
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
        adapter = new FileListAdapter(combinedList, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasStoragePermission()) {
            permissionView.setVisibility(View.GONE);
            loadFiles();
        } else {
            permissionView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onFileClick(int position) {
        if (position >= 0 && position < combinedList.size()) {
            Object clickedObject = combinedList.get(position);
            if (clickedObject instanceof FileItem) {
                FileItem fileItem = (FileItem) clickedObject;
                if (actionMode != null) {
                    toggleSelection(fileItem);
                } else {
                    openFile(fileItem);
                }
            } else {
                Log.w(TAG, "A non-file item was clicked at position: " + position);
            }
        }
    }

    @Override
    public void onFileLongClick(int position) {
        if (position >= 0 && position < combinedList.size()) {
            Object clickedObject = combinedList.get(position);
            if (clickedObject instanceof FileItem) {
                FileItem fileItem = (FileItem) clickedObject;
                if (actionMode == null) {
                    actionMode = toolbar.startActionMode(actionModeCallback);
                }
                toggleSelection(fileItem);
            }
        }
    }

    private void openFile(FileItem item) {
        try {
            Intent intent = new Intent(AllFilesActivity.this, PdfViewerActivity.class);
            File file = new File(item.path);
            String authority = getApplicationContext().getPackageName() + ".provider";
            Uri fileUri = FileProvider.getUriForFile(this, authority, file);

            intent.putExtra(PdfViewerActivity.EXTRA_FILE_URI, fileUri.toString());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "File Provider Error. Is your authority in AndroidManifest correct?", e);
            Toast.makeText(this, "Error: Could not open file.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void requestAdForPosition(int position) {
        if (!positionsCurrentlyLoading.contains(position) && !loadedAds.containsKey(position)) {
            positionsCurrentlyLoading.add(position);
            loadNativeAd(position);
        }
    }

    private void loadFiles() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        combinedList.clear();
        loadedAds.clear();
        positionsCurrentlyLoading.clear();
        adapter.notifyDataSetChanged();

        new Thread(() -> {
            List<FileItem> fileItems = new ArrayList<>();
            File root = Environment.getExternalStorageDirectory();
            searchPDFFilesRecursively(root, fileItems);
            Collections.sort(fileItems, (a, b) -> Long.compare(b.date, a.date));

            final List<Object> freshListWithPlaceholders = new ArrayList<>(fileItems);
            insertAdPlaceholders(freshListWithPlaceholders);

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (fileItems.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    combinedList.addAll(freshListWithPlaceholders);
                    adapter.notifyDataSetChanged();
                }
            });
        }).start();
    }

    private void insertAdPlaceholders(List<Object> list) {
        if (list.size() < FIRST_AD_POSITION) {
            return;
        }
        list.add(FIRST_AD_POSITION, null);
        for (int i = FIRST_AD_POSITION + 1 + AD_INTERVAL; i < list.size(); i += (AD_INTERVAL + 1)) {
            list.add(i, null);
        }
    }

    private void loadNativeAd(final int position) {
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        boolean isAdEnabled = remoteConfig.getBoolean("admob_native_ad_enabled");
        if (!isAdEnabled) {
            positionsCurrentlyLoading.remove(position);
            return;
        }

        String adUnitId = remoteConfig.getString("admob_native_ad_unit_id");
        if (adUnitId == null || adUnitId.isEmpty()) {
            adUnitId = "ca-app-pub-3940256099942544/2247696110";
        }

        AdLoader adLoader = new AdLoader.Builder(this, adUnitId)
                .forNativeAd(ad -> {
                    if (isDestroyed()) {
                        ad.destroy();
                        return;
                    }
                    runOnUiThread(() -> {
                        loadedAds.put(position, ad);
                        positionsCurrentlyLoading.remove(position);
                        if (position < combinedList.size() && combinedList.get(position) == null) {
                            combinedList.set(position, ad);
                            adapter.notifyItemChanged(position);
                        }
                    });
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Ad failed at position " + position + ": " + loadAdError.getMessage());
                        runOnUiThread(() -> {
                            positionsCurrentlyLoading.remove(position);
                            if (position < combinedList.size() && combinedList.get(position) == null) {
                                combinedList.remove(position);
                                adapter.notifyItemRemoved(position);
                            }
                        });
                    }
                }).build();

        adLoader.loadAd(new AdRequest.Builder().build());
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

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
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

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.setMultiSelectMode(false);
            adapter.clearSelections();
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
                    if (actionMode != null) {
                        actionMode.finish();
                    }
                    loadFiles();
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
        } else {
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        }
        intent.setType("application/pdf");

        startActivity(Intent.createChooser(intent, "Share PDF(s)"));
        if (actionMode != null) {
            actionMode.finish();
        }
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
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFiles();
            } else {
                Toast.makeText(this, "Read Permission is required to list files.", Toast.LENGTH_LONG).show();
                permissionView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (NativeAd ad : loadedAds.values()) {
            ad.destroy();
        }
        loadedAds.clear();
    }
}
