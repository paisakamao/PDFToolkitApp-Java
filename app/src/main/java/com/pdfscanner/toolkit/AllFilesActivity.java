package com.pdfscanner.toolkit;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AllFilesActivity extends AppCompatActivity implements FileListAdapter.OnFileClickListener {

    private RecyclerView recyclerView;
    private FileListAdapter adapter;
    private final List<FileItem> fileItems = new ArrayList<>();
    private boolean isMultiSelectMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_files);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // ✅ keep only this
        }

        recyclerView = findViewById(R.id.recycler_view_files);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FileListAdapter(fileItems, this);
        recyclerView.setAdapter(adapter);

        loadFiles();
        loadNativeAds();
    }

    private void loadFiles() {
        File directory = getExternalFilesDir(null);
        if (directory != null && directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    fileItems.add(new FileItem(file.getName(), file.length(), file.lastModified(), file.getAbsolutePath()));
                }
            }
        }
        adapter.setFiles(fileItems);

        // Insert loading placeholders every 7 items
        for (int i = 7; i < fileItems.size(); i += 8) {
            adapter.insertLoading(i);
        }
    }

    private void loadNativeAds() {
        String adUnitId = FirebaseRemoteConfig.getInstance().getString("admob_native_ad_unit_id"); // ✅ RemoteConfig

        AdLoader adLoader = new AdLoader.Builder(this, adUnitId)
                .forNativeAd(nativeAd -> {
                    // Replace first loading with ad
                    int position = findFirstLoadingPosition();
                    if (position != -1) {
                        adapter.replaceLoadingWithAd(position, nativeAd);
                    }
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

    private int findFirstLoadingPosition() {
        for (int i = 0; i < adapter.getItemCount(); i++) {
            if (adapter.getItemViewType(i) == 2) { // VIEW_TYPE_LOADING
                return i;
            }
        }
        return -1;
    }

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
        isMultiSelectMode = true;
        adapter.setMultiSelectMode(true);
        adapter.toggleSelection(item);
    }

    @Override
    public void onBackPressed() {
        if (isMultiSelectMode) {
            isMultiSelectMode = false;
            adapter.setMultiSelectMode(false);
        } else {
            super.onBackPressed();
        }
    }
}
