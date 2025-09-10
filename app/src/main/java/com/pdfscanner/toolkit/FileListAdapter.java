package com.pdfscanner.toolkit;

import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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

    private static final int VIEW_TYPE_FILE = 0;
    private static final int VIEW_TYPE_AD = 1;

    private final List<Object> items; // Can hold FileItem or NativeAd
    private final OnFileClickListener listener;
    private boolean isMultiSelectMode = false;
    private final Set<FileItem> selectedItems = new HashSet<>();

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
        if (items.get(position) instanceof FileItem) {
            return VIEW_TYPE_FILE;
        } else {
            return VIEW_TYPE_AD;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_FILE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
            return new FileViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_native_ad, parent, false);
            return new AdViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_FILE) {
            FileItem file = (FileItem) items.get(position);
            ((FileViewHolder) holder).bind(file);
            holder.itemView.setActivated(selectedItems.contains(file));
        } else {
            NativeAd nativeAd = (NativeAd) items.get(position);
            ((AdViewHolder) holder).bind(nativeAd);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ---------- Multi-select handling ----------
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
        notifyItemChanged(items.indexOf(item));
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

    // ---------- ViewHolders ----------

    class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView fileIcon;
        TextView fileName;
        TextView fileSize;
        TextView fileDate;

        FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.icon_file_type);
            fileName = itemView.findViewById(R.id.text_file_name);
            fileSize = itemView.findViewById(R.id.text_file_size);
            fileDate = itemView.findViewById(R.id.text_file_date);
        }

        void bind(final FileItem item) {
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
        FrameLayout adContainer;

        AdViewHolder(@NonNull View itemView) {
            super(itemView);
            adContainer = itemView.findViewById(R.id.ad_container);
        }

        void bind(NativeAd nativeAd) {
            NativeAdView adView = (NativeAdView) LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.list_item_ad_layout, null);
            populateNativeAdView(nativeAd, adView);

            adContainer.removeAllViews();
            adContainer.addView(adView);
        }

        private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
            // Map ad assets into the view layout (depends on list_item_ad_layout.xml setup)
            TextView headlineView = adView.findViewById(R.id.ad_headline);
            if (headlineView != null) {
                headlineView.setText(nativeAd.getHeadline());
                adView.setHeadlineView(headlineView);
            }

            adView.setNativeAd(nativeAd);
        }
    }
}
