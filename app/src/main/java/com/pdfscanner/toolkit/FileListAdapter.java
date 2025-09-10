package com.pdfscanner.toolkit;

import android.content.Context;
import android.net.Uri;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
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

    private final List<Object> items; // can be FileItem or NativeAd
    private final OnFileClickListener listener;
    private boolean isMultiSelectMode = false;
    private final Set<FileItem> selectedItems = new HashSet<>();

    public interface OnFileClickListener {
        void onFileClick(FileItem item);
        void onFileLongClick(FileItem item);
    }

    public FileListAdapter(List<FileItem> files, OnFileClickListener listener, Context context) {
        this.items = new ArrayList<>(files);
        this.listener = listener;
        loadNativeAds(context); // <-- load ads
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
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_file, parent, false);
            return new FileViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_ad_layout, parent, false);
            return new NativeAdViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FileViewHolder) {
            FileItem file = (FileItem) items.get(position);
            ((FileViewHolder) holder).bind(file);
            holder.itemView.setActivated(selectedItems.contains(file));
        } else if (holder instanceof NativeAdViewHolder) {
            NativeAd ad = (NativeAd) items.get(position);
            ((NativeAdViewHolder) holder).bind(ad);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // 🔹 Native Ad Loader
    private void loadNativeAds(Context context) {
        String adUnitId = FirebaseRemoteConfig.getInstance()
                .getString("admob_native_ad_unit_id");

        AdLoader adLoader = new AdLoader.Builder(context, adUnitId)
                .forNativeAd(nativeAd -> {
                    insertAdIntoList(nativeAd);
                    notifyDataSetChanged();
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull com.google.android.gms.ads.LoadAdError adError) {
                        Toast.makeText(context, "Ad failed: " + adError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .build();

        for (int i = 0; i < 2; i++) {
            adLoader.loadAd(new AdRequest.Builder().build());
        }
    }

    private void insertAdIntoList(NativeAd nativeAd) {
        int interval = 5; // show ad after every 5 items
        int nextAdPosition = interval;
        if (items.size() >= nextAdPosition) {
            items.add(nextAdPosition, nativeAd);
        } else {
            items.add(nativeAd);
        }
    }

    // ===============================
    // 🔹 File ViewHolder (unchanged)
    // ===============================
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

    // ===============================
    // 🔹 Native Ad ViewHolder
    // ===============================
    public static class NativeAdViewHolder extends RecyclerView.ViewHolder {
        NativeAdView adView;

        public NativeAdViewHolder(@NonNull View itemView) {
            super(itemView);
            adView = (NativeAdView) itemView;
        }

        void bind(NativeAd nativeAd) {
            adView.setNativeAd(nativeAd);
        }
    }
}
