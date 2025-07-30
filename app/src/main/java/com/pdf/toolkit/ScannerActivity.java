package com.pdf.toolkit;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton; // New import
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera; // New import
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton; // CRASH FIX
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScannerActivity extends AppCompatActivity {

    // --- All variables remain the same ---
    private static final int PERMISSIONS_REQUEST_CODE = 101;
    private static final String TAG = "ScannerActivity";
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private ExecutorService cameraExecutor;
    private ArrayList<String> capturedImagePaths = new ArrayList<>();
    private ImageCapture imageCapture;
    
    // --- New variables for Flashlight ---
    private Camera camera;
    private boolean isFlashOn = false;

    // --- UI Elements Updated ---
    private PreviewView previewView;
    private FloatingActionButton captureButton;
    private ExtendedFloatingActionButton doneButton; // CRASH FIX
    private TextView pageCountText;
    private CardView thumbnailPreviewCard;
    private ImageView thumbnailPreviewImage;
    private ImageButton flashButton; // New UI element

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        cameraExecutor = Executors.newSingleThreadExecutor();
        
        // Find all UI elements
        previewView = findViewById(R.id.camera_preview);
        captureButton = findViewById(R.id.capture_button);
        doneButton = findViewById(R.id.done_button);
        pageCountText = findViewById(R.id.page_count_text);
        thumbnailPreviewCard = findViewById(R.id.thumbnail_preview_card);
        thumbnailPreviewImage = findViewById(R.id.thumbnail_preview_image);
        flashButton = findViewById(R.id.flash_button); // Find new button

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
        }

        captureButton.setOnClickListener(v -> takePhoto());
        doneButton.setOnClickListener(v -> createPdf());
        // Set listener for the new flash button
        flashButton.setOnClickListener(v -> toggleFlash());
    }
    
    // --- New Method to Toggle Flashlight ---
    private void toggleFlash() {
        if (camera != null) {
            isFlashOn = !isFlashOn;
            camera.getCameraControl().enableTorch(isFlashOn);
            // Optionally, change the icon to indicate state
            flashButton.setAlpha(isFlashOn ? 1.0f : 0.5f);
        }
    }
    
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();

                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                cameraProvider.unbindAll();
                
                // --- Save the camera object to control flash ---
                this.camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (Exception e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // (The rest of your code - createPdf, takePhoto, etc. - remains exactly the same)
    private Bitmap getResizedBitmap(String imagePath, int maxSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        int srcWidth = options.outWidth;
        int srcHeight = options.outHeight;
        int newWidth = srcWidth;
        int newHeight = srcHeight;
        if (srcWidth > maxSize || srcHeight > maxSize) {
            if (srcWidth > srcHeight) {
                newWidth = maxSize;
                newHeight = (int) (srcHeight * ((float) maxSize / srcWidth));
            } else {
                newHeight = maxSize;
                newWidth = (int) (srcWidth * ((float) maxSize / srcHeight));
            }
        }
        int inSampleSize = 1;
        if (srcWidth > newWidth || srcHeight > newHeight) {
            final int halfWidth = srcWidth / 2;
            final int halfHeight = srcHeight / 2;
            while ((halfWidth / inSampleSize) >= newWidth && (halfHeight / inSampleSize) >= newHeight) {
                inSampleSize *= 2;
            }
        }
        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        Bitmap smallBitmap = BitmapFactory.decodeFile(imagePath, options);
        return Bitmap.createScaledBitmap(smallBitmap, newWidth, newHeight, true);
    }
    private void createPdf() {
        if (capturedImagePaths.isEmpty()) {
            Toast.makeText(this, "Capture at least one page first.", Toast.LENGTH_SHORT).show();
            return;
        }
        File pdfDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PDFToolkit");
        if (!pdfDir.exists()) pdfDir.mkdirs();
        String pdfFileName = "SCAN_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".pdf";
        File pdfFile = new File(pdfDir, pdfFileName);
        PdfDocument pdfDocument = new PdfDocument();
        for (String imagePath : capturedImagePaths) {
            try {
                Bitmap bitmap = getResizedBitmap(imagePath, 1024);
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), capturedImagePaths.indexOf(imagePath) + 1).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                page.getCanvas().drawBitmap(bitmap, 0, 0, null);
                pdfDocument.finishPage(page);
                bitmap.recycle();
            } catch (Exception e) {
                Log.e(TAG, "Error processing image " + imagePath, e);
            }
        }
        try {
            pdfDocument.writeTo(new FileOutputStream(pdfFile));
            Toast.makeText(this, "PDF saved: " + pdfFile.getName(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG, "Error writing PDF file", e);
            Toast.makeText(this, "Error saving PDF.", Toast.LENGTH_SHORT).show();
        } finally {
            pdfDocument.close();
            for (String imagePath : capturedImagePaths) {
                new File(imagePath).delete();
            }
            resetScannerState();
        }
    }
    private void takePhoto() {
        if (imageCapture == null) return;
        File photoDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PDFToolkit");
        if (!photoDir.exists()) photoDir.mkdirs();
        String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg";
        File photoFile = new File(photoDir, fileName);
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                String savedPath = photoFile.getAbsolutePath();
                capturedImagePaths.add(savedPath);
                runOnUiThread(() -> {
                    Bitmap bitmap = BitmapFactory.decodeFile(savedPath);
                    thumbnailPreviewImage.setImageBitmap(bitmap);
                    updatePageCount();
                    Toast.makeText(getBaseContext(), "Page " + capturedImagePaths.size() + " captured.", Toast.LENGTH_SHORT).show();
                });
            }
            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
            }
        });
    }
    private void updatePageCount() {
        int count = capturedImagePaths.size();
        pageCountText.setText("Pages: " + count);
        if (count > 0) {
            doneButton.setVisibility(View.VISIBLE);
            thumbnailPreviewCard.setVisibility(View.VISIBLE);
        } else {
            doneButton.setVisibility(View.INVISIBLE);
            thumbnailPreviewCard.setVisibility(View.INVISIBLE);
        }
    }
    private void resetScannerState() {
        capturedImagePaths.clear();
        updatePageCount();
    }
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to use the scanner.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
