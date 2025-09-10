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

        MobileAds.initialize(this, initializationStatus -> {});

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
        AdLoader adLoader = new AdLoader.Builder(this, getString(R.string.admob_native_ad_id))
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
        getMenuInflater().inflate(R.menu.menu_all_files, menu);
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
