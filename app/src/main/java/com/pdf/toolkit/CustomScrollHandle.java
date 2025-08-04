package com.pdf.toolkit;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.R;
import com.github.barteksc.pdfviewer.scroll.ScrollHandle;

// MODIFIED: We now implement the interface directly, not extend the default class.
public class CustomScrollHandle implements ScrollHandle {

    private final static int HANDLE_WIDTH = 65;
    private final static int HANDLE_HEIGHT = 40;
    private final static int DEFAULT_TEXT_SIZE = 16;

    private TextView textView;
    private Context context;
    protected PDFView pdfView;
    private float currentPos;
    private Handler handler = new Handler();
    private Runnable hidePageScrollerRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    public CustomScrollHandle(Context context) {
        this.context = context;
    }

    @Override
    public void setupLayout(PDFView pdfView) {
        this.pdfView = pdfView;
        LayoutInflater inflater = LayoutInflater.from(context);
        // We use our custom layout here
        textView = (TextView) inflater.inflate(com.pdf.toolkit.R.layout.custom_scroll_handle, pdfView, false);
        textView.setVisibility(View.INVISIBLE);
        pdfView.addView(textView);
    }

    @Override
    public void destroyLayout() {
        pdfView.removeView(textView);
    }

    @Override
    public void setScroll(float position) {
        if (!shown()) {
            show();
        } else {
            handler.removeCallbacks(hidePageScrollerRunnable);
        }
        if (pdfView != null) {
            setPosition((pdfView.getHeight() * position) - (HANDLE_HEIGHT / 2));
        }
    }

    private void setPosition(float y) {
        if (Float.isInfinite(y) || Float.isNaN(y)) {
            return;
        }
        float pdfViewHeight = pdfView.getHeight();
        y = Math.min(pdfViewHeight - HANDLE_HEIGHT, y);
        y = Math.max(0, y);

        // Position the handle on the right side of the screen
        float x = pdfView.getWidth() - textView.getWidth() - 8; // 8dp margin from right

        textView.setX(x);
        textView.setY(y);
        textView.invalidate();
        this.currentPos = y;
    }


    @Override
    public void setPageNum(int pageNum) {
        String text = String.valueOf(pageNum);
        if (!textView.getText().equals(text)) {
             // We get the total pages from the PDFView itself
            String fullText = String.format("%s / %s", pageNum, pdfView.getPageCount());
            textView.setText(fullText);
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

    @Override
    public void setTextColor(int color) {
        textView.setTextColor(color);
    }



    @Override
    public void setTextSize(int size) {
        textView.setTextSize(size);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!shown()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                pdfView.stopFling();
                handler.removeCallbacks(hidePageScrollerRunnable);
                if (event.getY() >= textView.getY() && event.getY() <= textView.getY() + textView.getHeight() &&
                        event.getX() >= textView.getX() && event.getX() <= textView.getX() + textView.getWidth()) {
                    pdfView.setPositionOffset(currentPos / pdfView.getHeight() - ( (event.getY() - currentPos) / pdfView.getZoom() / pdfView.getHeight()  ) , false);
                    return true;
                }
                return false;

            case MotionEvent.ACTION_MOVE:
                pdfView.setPositionOffset(currentPos / pdfView.getHeight() - ( (event.getY() - currentPos) / pdfView.getZoom() / pdfView.getHeight()  ) , false);
                return true;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                hideDelayed();
                pdfView.performPageSnap();
                return true;
        }

        return false;
    }
}
