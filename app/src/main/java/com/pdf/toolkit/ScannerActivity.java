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
import android.widget.ImageView; // New Import
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView; // New Import
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat; // New Import for Edge-to-Edge

import com.google.android.material.floatingactionbutton.FloatingActionButton; // New Import
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

    private static final int PERMISSIONS_REQUEST_CODE = 101;
    private static final String TAG = "ScannerActivity";
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private ExecutorService cameraExecutor;
    private ArrayList<String> capturedImagePaths = new ArrayList<>();
    private ImageCapture imageCapture;

    // --- UI Elements Updated ---
    private PreviewView previewView;
    private FloatingActionButton captureButton;
    private FloatingActionButton doneButton;
    private TextView pageCountText;
    private CardView thumbnailPreviewCard;
    private ImageView thumbnailPreviewImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This line enables edge-to-edge display. Must be called before setContentView.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_scanner);

        cameraExecutor = Executors.newSingleThreadExecutor();

        // --- Find all the new UI elements ---
        previewView = findViewById(R.id.camera_preview);
        captureButton = findViewById(R.id.capture_button);
        doneButton = findViewById(R.id.done_button);
        pageCountText = findViewById(R.id.page_count_text);
        thumbnailPreviewCard = findViewById(R.id.thumbnail_preview_card);
        thumbnailPreviewImage = findViewById(R.id.thumbnail_preview_image);


        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
        }

        captureButton.setOnClickListener(v -> takePhoto());
        doneButton.setOnClickListener(v -> createPdf());
    }

    private void takePhoto() {
        if (imageCapture == null) return;
        // ... (The file saving logic remains the same)
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

                // --- New UI Update Logic ---
                runOnUiThread(() -> {
                    // Load the captured image into a bitmap for the thumbnail
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
        // Show/hide buttons and thumbnail based on page count
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
        updatePageCount(); // This will now also hide the thumbnail
    }


    // --- All other methods (startCamera, createPdf, permissions, etc.) remain exactly the same as before. ---
    // (You can copy them from your previous version or from my last response, they don't need changes)

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
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (Exception e) {
                Log.e(TAG, "Use case binding failed", e);
                Toast.makeText(getApplicationContext(), "Failed to start camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void createPdf() {
        if (capturedImagePaths.isEmpty()) {
            Toast.makeText(this, "Capture at least one page first.", Toast.LENGTH_SHORT).show();
            return;
        }

        File pdfDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PDFToolkit");
        if (!pdfDir.exists()) {
            pdfDir.mkdirs();
        }
        String pdfFileName = "SCAN_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".pdf";
        File pdfFile = new File(pdfDir, pdfFileName);
        PdfDocument pdfDocument = new PdfDocument();

        for (String imagePath : capturedImagePaths) {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
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
            Toast.makeText(this, "PDF saved: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
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
