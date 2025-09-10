package com.pdfscanner.toolkit;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // --- View types ---
    private static final int VIEW_TYPE_FILE = 0;
    private static final int VIEW_TYPE_AD = 1;
    private static final int VIEW_TYPE_AD_LOADING = 2;

    private final List<FileItem> files;
    private final OnFileClickListener listener;
    private boolean isMultiSelectMode = false;
    private final Set<FileItem> selectedItems = new HashSet<>();

    private final Context context;

    // --- Ad management ---
    private final List<NativeAd> loadedAds = new ArrayList<>();
    private boolean isAdLoading = false;

    public interface OnFileClickListener {
        void onFileClick(FileItem item);
        void onFileLongClick(FileItem item);
    }

    public FileListAdapter(Context context, List<FileItem> files, OnFileClickListener listener) {
        this.context = context;
        this.files = files;
        this.listener = listener;

        // Preload first ad
        loadNativeAd();
    }

    @Override
    public int getItemViewType(int position) {
        // After 3rd item, then every 7 items
        if (shouldShowAdAtPosition(position)) {
            int adIndex = getAdIndexForPosition(position);
            if (adIndex < loadedAds.size()) {
                return VIEW_TYPE_AD;
            } else if (isAdLoading) {
                return VIEW_TYPE_AD_LOADING;
            } else {
                return VIEW_TYPE_FILE; // fallback to file if no ad
            }
        }
        return VIEW_TYPE_FILE;
    }

    private boolean shouldShowAdAtPosition(int position) {
        if (position == 3) return true;
        return position > 3 && (position - 3) % 7 == 0;
    }

    private int getAdIndexForPosition(int position) {
        if (position < 3) return -1;
        return ((position - 3) / 7);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_FILE) {
            View view = inflater.inflate(R.layout.item_file, parent, false);
            return new FileViewHolder(view);
        } else if (viewType == VIEW_TYPE_AD) {
            View view = inflater.inflate(R.layout.list_item_ad_layout, parent, false);
            return new AdViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.ad_loading_item, parent, false);
            return new AdLoadingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        if (viewType == VIEW_TYPE_FILE) {
            FileItem file = files.get(getFileIndex(position));
            ((FileViewHolder) holder).bind(file);
            holder.itemView.setActivated(selectedItems.contains(file));
        } else if (viewType == VIEW_TYPE_AD) {
            int adIndex = getAdIndexForPosition(position);
            if (adIndex >= 0 && adIndex < loadedAds.size()) {
                ((AdViewHolder) holder).bind(loadedAds.get(adIndex));
            }
        }
    }

    @Override
    public int getItemCount() {
        if (files == null) return 0;
        // Add extra items for ads
        int adSlots = 0;
        for (int i = 0; i < files.size(); i++) {
            if (shouldShowAdAtPosition(i)) {
                adSlots++;
            }
        }
        return files.size() + Math.min(adSlots, loadedAds.size() + (isAdLoading ? 1 : 0));
    }

    private int getFileIndex(int position) {
        // Adjust position to skip ads
        int adsBefore = 0;
        for (int i = 0; i < position; i++) {
            if (shouldShowAdAtPosition(i)) adsBefore++;
        }
        return position - adsBefore;
    }

    // --- Multi-select logic ---
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
        notifyItemChanged(files.indexOf(item));
    }

    public List<FileItem> getSelectedItems() {
        return new ArrayList<>(selectedItems);
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public void clearSelections() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    // --- ViewHolders ---

    class FileViewHolder extends RecyclerView.ViewHolder {
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

    class AdViewHolder extends RecyclerView.ViewHolder {
        NativeAdView adView;

        public AdViewHolder(@NonNull View itemView) {
            super(itemView);
            adView = (NativeAdView) itemView;
        }

        public void bind(NativeAd nativeAd) {
            TextView headline = adView.findViewById(R.id.ad_headline);
            TextView body = adView.findViewById(R.id.ad_body);
            ImageView icon = adView.findViewById(R.id.ad_app_icon);

            headline.setText(nativeAd.getHeadline());
            adView.setHeadlineView(headline);

            if (nativeAd.getBody() != null) {
                body.setText(nativeAd.getBody());
                adView.setBodyView(body);
            }

            if (nativeAd.getIcon() != null) {
                icon.setImageDrawable(nativeAd.getIcon().getDrawable());
                adView.setIconView(icon);
            }

            adView.setNativeAd(nativeAd);
        }
    }

    class AdLoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        public AdLoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(android.R.id.progress);
        }
    }

    // --- Load native ads ---
    private void loadNativeAd() {
        if (isAdLoading) return;
        isAdLoading = true;

        AdLoader adLoader = new AdLoader.Builder(context,
                "ca-app-pub-3940256099942544/2247696110") // TODO: replace with your ad unit id
                .forNativeAd(nativeAd -> {
                    loadedAds.add(nativeAd);
                    isAdLoading = false;
                    notifyDataSetChanged();

                    // Preload next ad
                    loadNativeAd();
                })
                .withAdListener(new com.google.android.gms.ads.AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        isAdLoading = false;
                    }
                })
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }
}
