package com.pdfscanner.toolkit;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Object> items;
    private final OnFileClickListener listener;
    private boolean isMultiSelectMode = false;
    private final Set<FileItem> selectedItems = new HashSet<>();

    private static final int VIEW_TYPE_FILE = 0;
    private static final int VIEW_TYPE_AD = 1;
    private static final int VIEW_TYPE_AD_LOADING = 2;

    public interface OnFileClickListener {
        void onFileClick(FileItem item);
        void onFileLongClick(FileItem item);
    }

    public FileListAdapter(List<Object> items, OnFileClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof FileItem) {
            return VIEW_TYPE_FILE;
        } else if (item instanceof NativeAd) {
            return VIEW_TYPE_AD;
        } else {
            return VIEW_TYPE_AD_LOADING;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_AD) {
            View adView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_ad_layout, parent, false);
            return new AdViewHolder(adView);
        } else if (viewType == VIEW_TYPE_AD_LOADING) {
            View loadingView = LayoutInflater.from(parent.getContext()).inflate(R.layout.ad_loading_item, parent, false);
            return new AdLoadingViewHolder(loadingView);
        } else {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
            return new FileViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_FILE) {
            FileItem file = (FileItem) items.get(position);
            ((FileViewHolder) holder).bind(file);
            holder.itemView.setActivated(selectedItems.contains(file));
        } else if (holder.getItemViewType() == VIEW_TYPE_AD) {
            NativeAd nativeAd = (NativeAd) items.get(position);
            populateNativeAdView(nativeAd, ((AdViewHolder) holder).getAdView());
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setMultiSelectMode(boolean multiSelectMode) {
        isMultiSelectMode = multiSelectMode;
        if (!isMultiSelectMode) {
            selectedItems.clear();
        }
        notifyDataSetChanged();
    }

    public void toggleSelection(FileItem item) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        } else {
            selectedItems.add(item);
        }
        for (int i=0; i < items.size(); i++) {
            if (item.equals(items.get(i))) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    public List<FileItem> getSelectedItems() { return new ArrayList<>(selectedItems); }
    public int getSelectedItemCount() { return selectedItems.size(); }
    public void clearSelections() { selectedItems.clear(); notifyDataSetChanged(); }

    public class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView fileIcon;
        TextView fileName;
        TextView fileSize;
        TextView fileDate;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.icon_file_type);
            fileName = itemView.findViewById(R.id.text_file_name);
            fileSize = itemView.findViewById(R.id.text_file_size);
            fileDate = itemView.findViewById(R.id.text_file_date);
        }

        public void bind(final FileItem item) {
            fileName.setText(item.name);
            fileSize.setText(Formatter.formatShortFileSize(itemView.getContext(), item.size));
            fileDate.setText(formatDate(item.date));
            fileIcon.setImageResource(R.drawable.ic_pdflist);
            
            // --- THIS IS YOUR ORIGINAL, RESTORED, CORRECT CLICK LOGIC ---
            itemView.setOnClickListener(v -> listener.onFileClick(item));
            itemView.setOnLongClickListener(v -> {
                listener.onFileLongClick(item);
                return true;
            });
        }

        private String formatDate(long millis) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            return sdf.format(new Date(millis));
        }
    }

    static class AdViewHolder extends RecyclerView.ViewHolder {
        private final NativeAdView adView;
        AdViewHolder(View view) {
            super(view);
            adView = (NativeAdView) view;
            adView.setIconView(adView.findViewById(R.id.ad_app_icon));
            adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
            adView.setBodyView(adView.findViewById(R.id.ad_body));
        }
        public NativeAdView getAdView() { return adView; }
    }

    static class AdLoadingViewHolder extends RecyclerView.ViewHolder {
        AdLoadingViewHolder(View view) {
            super(view);
        }
    }

    private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        if (nativeAd.getBody() == null) {
            adView.getBodyView().setVisibility(View.INVISIBLE);
        } else {
            adView.getBodyView().setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }
        if (nativeAd.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }
        adView.setNativeAd(nativeAd);
    }
}```

---

### **Step 4 of 4: The Full, Simplified `AllFilesActivity.java`**

This version replaces the complex and failing multi-ad logic with a **simple, robust logic that loads just one ad.** This will fix the endless spinner problem.

**Action:** Please **replace the entire contents** of your `app/src/main/java/com/pdfscanner/toolkit/AllFilesActivity.java` file with this new, final version.

```java
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
import java.util.List;

public class AllFilesActivity extends AppCompatActivity implements FileListAdapter.OnFileClickListener {

    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private static final String TAG = "AllFilesActivity";
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout permissionView;
    private TextView emptyView;

    private FileListAdapter adapter;
    private List<Object> combinedList = new ArrayList<>();
    private NativeAd nativeAd;
    private ActionMode actionMode;
    private Toolbar toolbar;
    private static final int AD_POSITION = 3;

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
        adapter = new FileListAdapter(this, combinedList, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasStoragePermission()) {
            permissionView.setVisibility(View.GONE);
            loadFilesAndAds();
        } else {
            permissionView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onFileClick(FileItem item) {
        if (actionMode != null) {
            toggleSelection(item);
        } else {
            Intent intent = new Intent(AllFilesActivity.this, PdfViewerActivity.class);
            File file = new File(item.path);
            Uri fileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
            intent.putExtra(PdfViewerActivity.EXTRA_FILE_URI, fileUri.toString());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
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
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
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
                loadFilesAndAds();
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

    private void loadFilesAndAds() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        new Thread(() -> {
            List<FileItem> fileItems = new ArrayList<>();
            File root = Environment.getExternalStorageDirectory();
            searchPDFFilesRecursively(root, fileItems);
            Collections.sort(fileItems, (a, b) -> Long.compare(b.date, a.date));

            final List<Object> freshCombinedList = new ArrayList<>(fileItems);
            
            if (freshCombinedList.size() >= AD_POSITION) {
                freshCombinedList.add(AD_POSITION, null);
            }

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                combinedList.clear();
                combinedList.addAll(freshCombinedList);
                adapter.notifyDataSetChanged();
                if (fileItems.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                    if (combinedList.contains(null)) {
                        loadNativeAd();
                    }
                }
            });
        }).start();
    }
    
    private void loadNativeAd() {
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        boolean isAdEnabled = remoteConfig.getBoolean("admob_native_ad_enabled");
        if (!isAdEnabled) return;
        
        String adUnitId = remoteConfig.getString("admob_native_ad_unit_id");
        if (adUnitId == null || adUnitId.isEmpty()) {
            adUnitId = "ca-app-pub-3940256099942544/2247696110";
        }
        
        AdLoader.Builder builder = new AdLoader.Builder(this, adUnitId);
        builder.forNativeAd(ad -> {
            if (isDestroyed()) {
                ad.destroy();
                return;
            }
            nativeAd = ad;
            int index = combinedList.indexOf(null);
            if (index != -1) {
                combinedList.set(index, nativeAd);
                adapter.notifyItemChanged(index);
            }
        });

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "Native ad failed to load: " + loadAdError.getMessage());
                int index = combinedList.indexOf(null);
                if (index != -1) {
                    combinedList.remove(index);
                    adapter.notifyItemRemoved(index);
                }
            }
        }).build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }
    
    private void searchPDFFilesRecursively(File dir, List<FileItem> fileList) { 
        if (dir == null || !dir.isDirectory()) return; 
        String dirPath = dir.getAbsolutePath(); 
        if (dirPath.contains("/.Trash") || dirPath.contains("/Android/data") || dirPath.contains("/.recycle")) { return; } 
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
            loadFilesAndAds(); 
        } else { 
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show(); 
        } 
    }
    
    @Override
    public boolean onSupportNavigateUp() { 
        onBackPressed(); 
        return true; 
    }

    @Override
    protected void onDestroy() {
        if (nativeAd != null) {
            nativeAd.destroy();
        }
        super.onDestroy();
    }
}
