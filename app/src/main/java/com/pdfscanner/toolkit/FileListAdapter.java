package com.yourpackage.scanner.toolkit;

import android.content.Context;
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

import java.util.List;

public class FileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_FILE = 0;
    private static final int VIEW_TYPE_AD = 1;
    private static final int VIEW_TYPE_AD_LOADING = 2;

    private final Context context;
    private final List<FileItem> fileList;
    private final OnFileClickListener listener;

    // To store loaded NativeAds
    private NativeAd firstAd = null;
    private NativeAd secondAd = null;

    public interface OnFileClickListener {
        void onFileClick(FileItem fileItem);
    }

    public FileListAdapter(Context context, List<FileItem> fileList, OnFileClickListener listener) {
        this.context = context;
        this.fileList = fileList;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        // First ad after 3 items
        if (position == 3) {
            return firstAd == null ? VIEW_TYPE_AD_LOADING : VIEW_TYPE_AD;
        }
        // Second ad after 7 more items (position 10)
        else if (position == 10) {
            return secondAd == null ? VIEW_TYPE_AD_LOADING : VIEW_TYPE_AD;
        } else {
            return VIEW_TYPE_FILE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == VIEW_TYPE_FILE) {
            View view = inflater.inflate(R.layout.list_item_file, parent, false);
            return new FileViewHolder(view);
        } else if (viewType == VIEW_TYPE_AD) {
            View adView = inflater.inflate(R.layout.list_item_ad_layout, parent, false);
            return new AdViewHolder(adView);
        } else {
            View loadingView = inflater.inflate(R.layout.ad_loading_item, parent, false);
            return new LoadingViewHolder(loadingView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);

        if (viewType == VIEW_TYPE_FILE) {
            int dataIndex = position;
            if (position > 10) dataIndex -= 2;
            else if (position > 3) dataIndex -= 1;

            FileItem fileItem = fileList.get(dataIndex);
            ((FileViewHolder) holder).bind(fileItem, listener);

        } else if (viewType == VIEW_TYPE_AD) {
            if (position == 3 && firstAd != null) {
                ((AdViewHolder) holder).bind(firstAd);
            } else if (position == 10 && secondAd != null) {
                ((AdViewHolder) holder).bind(secondAd);
            }
        } else if (viewType == VIEW_TYPE_AD_LOADING) {
            if (position == 3 && firstAd == null) {
                loadAdForPosition(3);
            } else if (position == 10 && secondAd == null) {
                loadAdForPosition(10);
            }
        }
    }

    @Override
    public int getItemCount() {
        int count = fileList.size();
        if (fileList.size() > 3) count++;
        if (fileList.size() > 10) count++;
        return count;
    }

    private void loadAdForPosition(int position) {
        AdLoader adLoader = new AdLoader.Builder(context, context.getString(R.string.admob_native_id))
                .forNativeAd(nativeAd -> {
                    if (position == 3) {
                        firstAd = nativeAd;
                        notifyItemChanged(3);
                    } else if (position == 10) {
                        secondAd = nativeAd;
                        notifyItemChanged(10);
                    }
                })
                .withAdListener(new com.google.android.gms.ads.AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        // Do nothing, just skip ad
                    }
                })
                .build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileName;
        ImageView fileIcon;

        FileViewHolder(View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.fileName);
            fileIcon = itemView.findViewById(R.id.fileIcon);
        }

        void bind(FileItem fileItem, OnFileClickListener listener) {
            fileName.setText(fileItem.getName());
            itemView.setOnClickListener(v -> listener.onFileClick(fileItem));
        }
    }

    static class AdViewHolder extends RecyclerView.ViewHolder {
        NativeAdView adView;
        TextView adHeadline;
        TextView adBody;
        ImageView adIcon;

        AdViewHolder(View itemView) {
            super(itemView);
            adView = (NativeAdView) itemView;
            adHeadline = itemView.findViewById(R.id.ad_headline);
            adBody = itemView.findViewById(R.id.ad_body);
            adIcon = itemView.findViewById(R.id.ad_app_icon);

            adView.setHeadlineView(adHeadline);
            adView.setBodyView(adBody);
            adView.setIconView(adIcon);
        }

        void bind(NativeAd nativeAd) {
            adHeadline.setText(nativeAd.getHeadline());
            adView.getHeadlineView().setVisibility(View.VISIBLE);

            if (nativeAd.getBody() != null) {
                adBody.setText(nativeAd.getBody());
                adView.getBodyView().setVisibility(View.VISIBLE);
            } else {
                adView.getBodyView().setVisibility(View.INVISIBLE);
            }

            if (nativeAd.getIcon() != null) {
                ((ImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
                adView.getIconView().setVisibility(View.VISIBLE);
            } else {
                adView.getIconView().setVisibility(View.GONE);
            }

            adView.setNativeAd(nativeAd);
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        LoadingViewHolder(View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}
