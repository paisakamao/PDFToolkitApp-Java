package com.pdf.toolkit;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class ScannerActivity extends AppCompatActivity {

    // A constant to identify our permission request
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    // A list to hold the locations (URIs) of all captured images
    private ArrayList<String> capturedImagePaths = new ArrayList<>();

    // Variables for the camera and the thread pool
    private ExecutorService cameraExecutor;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    // UI Elements from our layout file
    private PreviewView previewView;
    private ImageButton captureButton;
    private Button doneButton;
    private TextView pageCountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        // Find the UI elements by their ID from the XML layout
        previewView = findViewById(R.id.camera_preview);
        captureButton = findViewById(R.id.capture_button);
        doneButton = findViewById(R.id.done_button);
        pageCountText = findViewById(R.id.page_count_text);

        // Check if we already have camera permission
        if (allPermissionsGranted()) {
            // If yes, start the camera right away
            startCamera();
        } else {
            // If no, ask the user for permission
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE
            );
        }

        // We will add logic to the buttons in the next steps
        captureButton.setOnClickListener(v -> takePhoto());
        doneButton.setOnClickListener(v -> createPdf());
    }

    // This method checks if the CAMERA permission has been granted.
    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                getBaseContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    // This method is called automatically after the user responds to the permission request dialog.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                // The user granted permission. Start the camera.
                startCamera();
            } else {
                // The user denied permission. Show a message and close the scanner.
                Toast.makeText(this, "Camera permission is required to use the scanner.", Toast.LENGTH_LONG).show();
                finish(); // Go back to the previous screen
            }
        }
    }

    // --- We will fill in these methods in the next steps ---

    private void startCamera() {
        // Code to set up and start the camera preview will go here.
        Toast.makeText(this, "Camera Started (Placeholder)", Toast.LENGTH_SHORT).show();
    }

    private void takePhoto() {
        // Code to capture an image will go here.
        Toast.makeText(this, "Photo Taken (Placeholder)", Toast.LENGTH_SHORT).show();
    }

    private void createPdf() {
        // Code to convert captured images to a PDF will go here.
        Toast.makeText(this, "PDF Created (Placeholder)", Toast.LENGTH_SHORT).show();
    }
}
