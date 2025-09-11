// File Location: app/src/main/java/com/pdfscanner/toolkit/FileListAdapter.java
package com.pdfscanner.toolkit;

import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.nativead.MediaView;
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
    private final OnFileInteractionListener listener;
    private boolean isMultiSelectMode = false;
    private final Set<FileItem> selectedItems = new HashSet<>();

    private static final int VIEW_TYPE_FILE = 0;
    private static final int VIEW_TYPE_AD = 1;
    private static final int VIEW_TYPE_AD_LOADING = 2;

    public interface OnFileInteractionListener {
        void onFileClick(FileItem item);
        void onFileLongClick(FileItem item);
        void requestAdForPosition(int position);
    }

    public FileListAdapter(List<Object> items, OnFileInteractionListener listener) {
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
        } else { // Handles null or any other object as a loading placeholder
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
        int viewType = getItemViewType(position);
        if (viewType == VIEW_TYPE_FILE) {
            FileItem file = (FileItem) items.get(position);
            ((FileViewHolder) holder).bind(file);
            holder.itemView.setActivated(selectedItems.contains(file));
        } else if (viewType == VIEW_TYPE_AD) {
            NativeAd nativeAd = (NativeAd) items.get(position);
            populateNativeAdView(nativeAd, ((AdViewHolder) holder).getAdView());
        } else if (viewType == VIEW_TYPE_AD_LOADING) {
            if (listener != null) {
                listener.requestAdForPosition(holder.getAdapterPosition());
            }
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

            itemView.setOnClickListener(v -> {
                if(listener != null) listener.onFileClick(item);
            });
            itemView.setOnLongClickListener(v -> {
                if(listener != null) listener.onFileLongClick(item);
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
            adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
            adView.setBodyView(adView.findViewById(R.id.ad_body));
            adView.setIconView(adView.findViewById(R.id.ad_app_icon));
            adView.setMediaView(view.findViewById(R.id.ad_media));
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
            adView.getIconView().setVisibility(View.VISIBLE);
            ((ImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
        }

        if (nativeAd.getMediaContent() != null) {
            adView.getMediaView().setVisibility(View.VISIBLE);
            adView.getMediaView().setMediaContent(nativeAd.getMediaContent());
        } else {
            adView.getMediaView().setVisibility(View.GONE);
        }

        adView.setNativeAd(nativeAd);
    }
}
