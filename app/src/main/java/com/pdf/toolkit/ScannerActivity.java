package com.pdf.toolkit;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.lifecycle.ProcessCameraProvider; // <--- THE FIX IS HERE
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors; // We also need this import

public class ScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private ArrayList<String> capturedImagePaths = new ArrayList<>();
    private ExecutorService cameraExecutor;

    // UI Elements
    private PreviewView previewView;
    private ImageButton captureButton;
    private Button doneButton;
    private TextView pageCountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        // Initialize our background thread executor
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Find UI elements
        previewView = findViewById(R.id.camera_preview);
        captureButton = findViewById(R.id.capture_button);
        doneButton = findViewById(R.id.done_button);
        pageCountText = findViewById(R.id.page_count_text);

        // Check for camera permission
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }

        // Set up button listeners
        captureButton.setOnClickListener(v -> takePhoto());
        doneButton.setOnClickListener(v -> createPdf());
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
        // Important to shut down the executor when the activity is destroyed
        cameraExecutor.shutdown();
    }

    // --- We will fill these placeholder methods next ---

    private void startCamera() {
        Toast.makeText(this, "Camera Started (Placeholder)", Toast.LENGTH_SHORT).show();
    }

    private void takePhoto() {
        Toast.makeText(this, "Photo Taken (Placeholder)", Toast.LENGTH_SHORT).show();
    }

    private void createPdf() {
        Toast.makeText(this, "PDF Created (Placeholder)", Toast.LENGTH_SHORT).show();
    }
}
