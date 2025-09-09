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

    // The list now holds generic Objects to accommodate files, ads, and placeholders
    private final List<Object> items;
    private final OnFileClickListener listener;
    private final Context context;

    private boolean isMultiSelectMode = false;
    private final Set<FileItem> selectedItems = new HashSet<>();

    // View type constants
    private static final int VIEW_TYPE_FILE = 0;
    private static final int VIEW_TYPE_AD = 1;
    private static final int VIEW_TYPE_AD_LOADING = 2;

    public interface OnFileClickListener {
        void onFileClick(FileItem item);
        void onFileLongClick(FileItem item);
    }

    // The constructor now accepts a List<Object>
    public FileListAdapter(Context context, List<Object> items, OnFileClickListener listener) {
        this.context = context;
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
        } else { // VIEW_TYPE_FILE
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
        // No binding needed for the loading placeholder
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
    
    // --- MULTI-SELECT METHODS (Unchanged but adapted for List<Object>) ---
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
        // Find the correct index to notify, as items is a List<Object>
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

    // --- FILE VIEWHOLDER (Your original, inner class) ---
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
                if (isMultiSelectMode) {
                    listener.onFileLongClick(item); // Treat click as long click in multi-select mode
                } else {
                    listener.onFileClick(item);
                }
            });
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

    // --- NEW VIEWHOLDERS FOR ADS ---
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
}