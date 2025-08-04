package com.pdf.toolkit;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.scroll.ScrollHandle;

public class CustomScrollHandle implements ScrollHandle {

    private TextView textView;
    private Context context;
    protected PDFView pdfView;
    private float currentPos;
    private Handler handler = new Handler();
    private Runnable hidePageScrollerRunnable = this::hide;

    public CustomScrollHandle(Context context) {
        this.context = context;
    }

    @Override
    public void setupLayout(PDFView pdfView) {
        this.pdfView = pdfView;
        LayoutInflater inflater = LayoutInflater.from(context);
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
            setPosition((pdfView.getHeight() * position) - (textView.getHeight() / 2f));
        }
    }

    private void setPosition(float y) {
        if (Float.isInfinite(y) || Float.isNaN(y)) {
            return;
        }
        float pdfViewHeight = pdfView.getHeight();
        y = Math.max(0, y);
        y = Math.min(pdfViewHeight - textView.getHeight(), y);
        float x = pdfView.getWidth() - textView.getWidth() - 16;
        textView.setX(x);
        textView.setY(y);
        textView.invalidate();
        this.currentPos = y;
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
                    float relativeY = event.getY() - currentPos;
                    pdfView.setPositionOffset(relativeY / pdfView.getHeight(), false);
                    return true;
                }
                return false;

            case MotionEvent.ACTION_MOVE:
                float relativeYMove = event.getY() - currentPos;
                pdfView.setPositionOffset(relativeYMove / pdfView.getHeight(), false);
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
