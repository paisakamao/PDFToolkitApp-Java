package com.pdfscanner.toolkit;

import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
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

    private static final int ITEM_TYPE_FILE = 0;
    private static final int ITEM_TYPE_AD = 1;
    private static final int ITEM_TYPE_AD_LOADING = 2;

    private final List<Object> items; // Can hold FileItem, NativeAd, or "LOADING_AD"
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
        Object item = items.get(position);
        if (item instanceof FileItem) return ITEM_TYPE_FILE;
        else if (item instanceof NativeAd) return ITEM_TYPE_AD;
        else return ITEM_TYPE_AD_LOADING;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ITEM_TYPE_FILE) {
            View view = inflater.inflate(R.layout.item_file, parent, false);
            return new FileViewHolder(view);
        } else if (viewType == ITEM_TYPE_AD) {
            View view = inflater.inflate(R.layout.list_item_ad_layout, parent, false);
            return new AdViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.ad_loading_item, parent, false);
            return new LoadingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);

        if (holder instanceof FileViewHolder && item instanceof FileItem) {
            ((FileViewHolder) holder).bind((FileItem) item);
            holder.itemView.setActivated(selectedItems.contains(item));
        } else if (holder instanceof AdViewHolder && item instanceof NativeAd) {
            populateNativeAdView((NativeAd) item, ((AdViewHolder) holder).adView);
        } else if (holder instanceof LoadingViewHolder) {
            // ProgressBar already in layout
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ------------------ Multi Select ------------------
    public void setMultiSelectMode(boolean multiSelectMode) {
        isMultiSelectMode = multiSelectMode;
        if (!isMultiSelectMode) selectedItems.clear();
        notifyDataSetChanged();
    }

    public void toggleSelection(FileItem item) {
        if (selectedItems.contains(item)) selectedItems.remove(item);
        else selectedItems.add(item);
        notifyItemChanged(items.indexOf(item));
    }

    public List<FileItem> getSelectedItems() { return new ArrayList<>(selectedItems); }
    public int getSelectedItemCount() { return selectedItems.size(); }
    public void clearSelections() { selectedItems.clear(); notifyDataSetChanged(); }

    // ------------------ View Holders ------------------
    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView fileIcon;
        TextView fileName, fileSize, fileDate;

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
            fileDate.setText(new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(new Date(item.date)));
            fileIcon.setImageResource(R.drawable.ic_pdflist);

            itemView.setOnClickListener(v -> ((OnFileClickListener) itemView.getContext()).onFileClick(item));
            itemView.setOnLongClickListener(v -> {
                ((OnFileClickListener) itemView.getContext()).onFileLongClick(item);
                return true;
            });
        }
    }

    static class AdViewHolder extends RecyclerView.ViewHolder {
        NativeAdView adView;
        AdViewHolder(@NonNull View itemView) {
            super(itemView);
            adView = (NativeAdView) itemView;
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;
        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(android.R.id.progress);
        }
    }

    // ------------------ Native Ad Binder ------------------
    private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        TextView headline = adView.findViewById(R.id.ad_headline);
        TextView body = adView.findViewById(R.id.ad_body);
        ImageView icon = adView.findViewById(R.id.ad_app_icon);

        adView.setHeadlineView(headline);
        headline.setText(nativeAd.getHeadline());

        if (nativeAd.getBody() != null) {
            adView.setBodyView(body);
            body.setText(nativeAd.getBody());
        }

        if (nativeAd.getIcon() != null) {
            adView.setIconView(icon);
            icon.setImageDrawable(nativeAd.getIcon().getDrawable());
        }

        adView.setNativeAd(nativeAd);
    }
}
