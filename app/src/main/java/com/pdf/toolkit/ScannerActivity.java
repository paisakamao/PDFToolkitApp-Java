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
import android.widget.Button;
import android.widget.ImageButton;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

    // UI Elements
    private PreviewView previewView;
    private ImageButton captureButton;
    private Button doneButton;
    private TextView pageCountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        cameraExecutor = Executors.newSingleThreadExecutor();
        previewView = findViewById(R.id.camera_preview);
        captureButton = findViewById(R.id.capture_button);
        doneButton = findViewById(R.id.done_button);
        pageCountText = findViewById(R.id.page_count_text);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
        }

        captureButton.setOnClickListener(v -> takePhoto());
        doneButton.setOnClickListener(v -> createPdf());
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
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (Exception e) {
                Log.e(TAG, "Use case binding failed", e);
                Toast.makeText(getApplicationContext(), "Failed to start camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
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

    private void createPdf() {
        if (capturedImagePaths.isEmpty()) {
            Toast.makeText(this, "Capture at least one page first.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a directory for our PDFs in the public Documents folder
        File pdfDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PDFToolkit");
        if (!pdfDir.exists()) {
            pdfDir.mkdirs();
        }

        String pdfFileName = "SCAN_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".pdf";
        File pdfFile = new File(pdfDir, pdfFileName);

        // Create a new PDF document
        PdfDocument pdfDocument = new PdfDocument();

        for (String imagePath : capturedImagePaths) {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);

                // Create a page with the same dimensions as the image
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), capturedImagePaths.indexOf(imagePath) + 1).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);

                // Draw the bitmap onto the page's canvas
                page.getCanvas().drawBitmap(bitmap, 0, 0, null);
                pdfDocument.finishPage(page);

                // Recycle the bitmap to save memory
                bitmap.recycle();
            } catch (Exception e) {
                Log.e(TAG, "Error processing image " + imagePath, e);
            }
        }

        try {
            // Write the PDF document to the file
            pdfDocument.writeTo(new FileOutputStream(pdfFile));
            Toast.makeText(this, "PDF saved: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG, "Error writing PDF file", e);
            Toast.makeText(this, "Error saving PDF.", Toast.LENGTH_SHORT).show();
        } finally {
            // Close the document
            pdfDocument.close();
            // Clean up the temporary image files
            for (String imagePath : capturedImagePaths) {
                new File(imagePath).delete();
            }
            // Reset the scanner for the next use
            resetScannerState();
        }
    }

    private void updatePageCount() {
        int count = capturedImagePaths.size();
        pageCountText.setText("Pages: " + count);
        doneButton.setVisibility(count > 0 ? View.VISIBLE : View.INVISIBLE);
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
