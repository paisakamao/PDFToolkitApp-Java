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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_FILE = 0;
    private static final int VIEW_TYPE_AD = 1;
    private static final int VIEW_TYPE_LOADING = 2;

    private final List<Object> items = new ArrayList<>();
    private final OnFileClickListener listener;

    // 🔹 Multi-select tracking
    private boolean isMultiSelectMode = false;
    private final Set<FileItem> selectedItems = new HashSet<>();

    public interface OnFileClickListener {
        void onFileClick(FileItem item);
        void onFileLongClick(FileItem item);
    }

    public FileListAdapter(List<FileItem> fileList, OnFileClickListener listener) {
        this.listener = listener;
        items.addAll(fileList);
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof FileItem) {
            return VIEW_TYPE_FILE;
        } else if (item instanceof NativeAd) {
            return VIEW_TYPE_AD;
        } else if (item instanceof LoadingItem) {
            return VIEW_TYPE_LOADING;
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        return items.size();
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
            return new LoadingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);

        if (holder instanceof FileViewHolder && item instanceof FileItem) {
            ((FileViewHolder) holder).bind((FileItem) item, listener, isMultiSelectMode, selectedItems.contains(item));
        } else if (holder instanceof AdViewHolder && item instanceof NativeAd) {
            ((AdViewHolder) holder).bind((NativeAd) item);
        } else if (holder instanceof LoadingViewHolder) {
            ((LoadingViewHolder) holder).bind();
        }
    }

    // -------------------- Multi-select methods --------------------
    public void setMultiSelectMode(boolean multiSelectMode) {
        isMultiSelectMode = multiSelectMode;
        if (!multiSelectMode) {
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

    // -------------------- Public helpers --------------------
    public void setFiles(List<FileItem> fileList) {
        items.clear();
        items.addAll(fileList);
        notifyDataSetChanged();
    }

    public void insertAd(NativeAd ad, int position) {
        if (position >= 0 && position <= items.size()) {
            items.add(position, ad);
            notifyItemInserted(position);
        }
    }

    public void insertLoading(int position) {
        if (position >= 0 && position <= items.size()) {
            items.add(position, new LoadingItem());
            notifyItemInserted(position);
        }
    }

    public void replaceLoadingWithAd(int position, NativeAd ad) {
        if (position >= 0 && position < items.size() && items.get(position) instanceof LoadingItem) {
            items.set(position, ad);
            notifyItemChanged(position);
        }
    }

    // -------------------- ViewHolders --------------------
    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView name, size, date;
        ImageView icon;

        FileViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_file_name);
            size = itemView.findViewById(R.id.text_file_size);
            date = itemView.findViewById(R.id.text_file_date);
            icon = itemView.findViewById(R.id.icon_file_type);
        }

        void bind(final FileItem file, final OnFileClickListener listener,
                  boolean isMultiSelectMode, boolean isSelected) {
            name.setText(file.name);
            size.setText(Formatter.formatShortFileSize(itemView.getContext(), file.size));
            date.setText(DateFormat.getDateTimeInstance().format(new Date(file.date)));
            icon.setImageResource(R.drawable.ic_pdflist);

            itemView.setActivated(isSelected);

            itemView.setOnClickListener(v -> listener.onFileClick(file));
            itemView.setOnLongClickListener(v -> {
                listener.onFileLongClick(file);
                return true;
            });
        }
    }

    static class AdViewHolder extends RecyclerView.ViewHolder {
        NativeAdView adView;
        TextView headline, body;
        ImageView icon;

        AdViewHolder(@NonNull View itemView) {
            super(itemView);
            adView = (NativeAdView) itemView;

            icon = itemView.findViewById(R.id.ad_app_icon);
            headline = itemView.findViewById(R.id.ad_headline);
            body = itemView.findViewById(R.id.ad_body);

            adView.setIconView(icon);
            adView.setHeadlineView(headline);
            adView.setBodyView(body);
        }

        void bind(NativeAd ad) {
            ((TextView) adView.getHeadlineView()).setText(ad.getHeadline());

            if (ad.getBody() != null) {
                ((TextView) adView.getBodyView()).setText(ad.getBody());
                adView.getBodyView().setVisibility(View.VISIBLE);
            } else {
                adView.getBodyView().setVisibility(View.GONE);
            }

            if (ad.getIcon() != null) {
                ((ImageView) adView.getIconView()).setImageDrawable(ad.getIcon().getDrawable());
                adView.getIconView().setVisibility(View.VISIBLE);
            } else {
                adView.getIconView().setVisibility(View.GONE);
            }

            adView.setNativeAd(ad);
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progress_bar);
        }

        void bind() {
            progressBar.setIndeterminate(true);
        }
    }

    // Dummy class for Loading item
    static class LoadingItem { }
}
