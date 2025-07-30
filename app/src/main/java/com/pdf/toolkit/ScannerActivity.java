package com.pdf.toolkit;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log; // Import Log for debugging
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "ScannerActivity"; // Tag for logging

    private ArrayList<String> capturedImagePaths = new ArrayList<>();
    private ExecutorService cameraExecutor;

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
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }

        captureButton.setOnClickListener(v -> takePhoto());
        doneButton.setOnClickListener(v -> createPdf());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // CameraProvider is now available.
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Build the Preview use case
                Preview preview = new Preview.Builder().build();

                // Select the back camera by default
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Attach the PreviewView's surface provider to the preview use case
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Unbind everything before rebinding
                cameraProvider.unbindAll();

                // Bind the camera provider to the lifecycle of this activity
                cameraProvider.bindToLifecycle(this, cameraSelector, preview);

            } catch (Exception e) {
                // Log any errors
                Log.e(TAG, "Use case binding failed", e);
                Toast.makeText(this, "Failed to start camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }


    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                getBaseContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
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

    // --- We will fill these placeholder methods next ---

    private void takePhoto() {
        Toast.makeText(this, "Photo Taken (Placeholder)", Toast.LENGTH_SHORT).show();
    }

    private void createPdf() {
        Toast.makeText(this, "PDF Created (Placeholder)", Toast.LENGTH_SHORT).show();
    }
}
