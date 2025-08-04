package com.pdf.toolkit;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TextView;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;

// We extend DefaultScrollHandle to get all the show/hide logic for free
public class CustomScrollHandle extends DefaultScrollHandle {

    private TextView textView;

    public CustomScrollHandle(Context context) {
        super(context);
    }

    @Override
    protected void setupLayout(PDFView pdfView) {
        // Inflate our custom layout instead of the default one
        LayoutInflater inflater = LayoutInflater.from(context);
        root = inflater.inflate(R.layout.custom_scroll_handle, pdfView, false);
        this.pdfView = pdfView;
        textView = root.findViewById(R.id.scrollHandleTextView);
        pdfView.addView(root);
    }

    /**
     * This is the crucial method. The library calls this to update the page number.
     * We will format the text exactly as we want: "current / total".
     */
    @Override
    public void setPageNum(int pageNum) {
        if (pdfView != null) {
            String text = String.format("%s / %s", pageNum, pdfView.getPageCount());
            textView.setText(text);
        }
    }

    // We can leave destroyLayout, show, hide, and setScroll as they are in the parent class.
}
