package com.pdf.toolkit;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.scroll.ScrollHandle;

// MODIFIED: This class now correctly implements the ScrollHandle interface
// without the extra methods that caused the build to fail.
public class CustomScrollHandle implements ScrollHandle {

    private TextView textView;
    private Context context;
    protected PDFView pdfView;
    private Handler handler = new Handler();
    private Runnable hidePageScrollerRunnable = () -> hide();

    public CustomScrollHandle(Context context) {
        this.context = context;
    }

    @Override
    public void setupLayout(PDFView pdfView) {
        this.pdfView = pdfView;
        LayoutInflater inflater = LayoutInflater.from(context);
        // Use our custom layout
        textView = (TextView) inflater.inflate(com.pdf.toolkit.R.layout.custom_scroll_handle, pdfView, false);
        textView.setVisibility(View.INVISIBLE);
        pdfView.addView(textView);
    }

    @Override
    public void destroyLayout() {
        if (pdfView != null) {
            pdfView.removeView(textView);
        }
    }

    @Override
    public void setScroll(float position) {
        if (!shown()) {
            show();
        } else {
            handler.removeCallbacks(hidePageScrollerRunnable);
        }

        if (pdfView != null) {
            // Calculate the Y position for the handle
            float pdfViewHeight = pdfView.getHeight();
            float handleHeight = textView.getHeight();
            if (handleHeight <= 0) { // If not measured yet, use a default
                handleHeight = 40 * context.getResources().getDisplayMetrics().density; // 40dp approx
            }

            float y = (pdfViewHeight * position) - (handleHeight / 2);

            // Clamp the value to stay within the bounds of the PDFView
            y = Math.max(0, y);
            y = Math.min(pdfViewHeight - handleHeight, y);

            // Position the handle on the right side of the screen
            float x = pdfView.getWidth() - textView.getWidth() - 16; // 16px margin from right edge

            textView.setX(x);
            textView.setY(y);
            textView.invalidate();
        }
    }

    @Override
    public void setPageNum(int pageNum) {
        if (pdfView != null && pdfView.getPageCount() > 0) {
            String text = String.format("%s / %s", pageNum, pdfView.getPageCount());
            if (!textView.getText().equals(text)) {
                textView.setText(text);
            }
        }
    }

    @Override
    public boolean shown() {
        return textView.getVisibility() == View.VISIBLE;
    }

    @Override
    public void show() {
        textView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hide() {
        textView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void hideDelayed() {
        handler.postDelayed(hidePageScrollerRunnable, 1000);
    }
}
