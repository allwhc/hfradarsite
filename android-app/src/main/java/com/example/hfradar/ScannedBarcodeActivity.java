package com.example.hfradar;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import java.io.IOException;
import java.io.InputStream;

public class ScannedBarcodeActivity extends Fragment {

    private View view;
    private SurfaceView surfaceView;
    private TextView txtBarcodeValue;
    private Button btnCancel;
    private Button btnGallery;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private static final int REQUEST_IMAGE_PICK = 202;
    private String intentData = "";
    public static final String MyPREFERENCES = "ScanPrefs";
    private SharedPreferences sharedpreferences;
    private boolean scanProcessed = false; // Flag to prevent multiple scans
    private Handler mainHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.activity_scanned_barcode, container, false);
        mainHandler = new Handler(Looper.getMainLooper());
        initViews();
        return view;
    }

    private void initViews() {
        txtBarcodeValue = view.findViewById(R.id.txtBarcodeValue);
        surfaceView = view.findViewById(R.id.surfaceView);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnGallery = view.findViewById(R.id.btnGallery);

        // Cancel button - return to Site Report without scanning
        btnCancel.setOnClickListener(v -> {
            // Set flag to indicate scan was cancelled (not successful)
            sharedpreferences = getActivity().getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putString("scan_cancelled", "1");
            editor.apply();
            navigateBack(false);
        });

        // Gallery button - pick image from gallery
        btnGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_IMAGE_PICK);
        });
    }

    private void initialiseDetectorsAndSources() {
        if (getActivity() == null) return;

        barcodeDetector = new BarcodeDetector.Builder(getActivity())
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        cameraSource = new CameraSource.Builder(getActivity(), barcodeDetector)
                .setRequestedPreviewSize(1920, 1080)  // Higher resolution for better QR detection
                .setAutoFocusEnabled(true)
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (getActivity() != null && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(surfaceView.getHolder());
                    } else if (getActivity() != null) {
                        ActivityCompat.requestPermissions(getActivity(), new
                                String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (cameraSource != null) {
                    cameraSource.stop();
                }
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0 && !scanProcessed) {
                    // Get the barcode value
                    String barcodeValue = barcodes.valueAt(0).displayValue;
                    if (barcodeValue != null && !barcodeValue.isEmpty()) {
                        // Mark as processed to prevent multiple scans
                        scanProcessed = true;
                        intentData = barcodeValue;

                        // Update UI and navigate on main thread
                        mainHandler.post(() -> {
                            onScanSuccess();
                        });
                    }
                }
            }
        });
    }

    private void onScanSuccess() {
        if (getActivity() == null) return;

        // Update status text
        txtBarcodeValue.setText("Scan successful!");
        txtBarcodeValue.setTextColor(0xFF4CAF50); // Green

        // Save scan data and clear any cancelled flag
        sharedpreferences = getActivity().getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString("scan_data", intentData);
        editor.putString("scan_data1", "1");
        editor.putString("scan_cancelled", "0");
        editor.apply();

        // Show brief success message then navigate
        Toast.makeText(getActivity(), "QR scanned successfully!", Toast.LENGTH_SHORT).show();

        // Navigate after a brief delay so user sees success message
        mainHandler.postDelayed(() -> {
            navigateBack(true);
        }, 500);
    }

    private void navigateBack(boolean withData) {
        if (getActivity() == null) return;

        // Stop camera
        if (cameraSource != null) {
            cameraSource.stop();
        }

        // Navigate to HomeInventory
        Fragment frag = new HomeInventory();
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frame, frag);
        transaction.commit();

        if (withData) {
            Toast.makeText(getActivity(), "Review and edit data before saving.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                processImageFromGallery(imageUri);
            }
        }
    }

    private void processImageFromGallery(Uri imageUri) {
        if (getActivity() == null) return;

        try {
            // Load bitmap from URI
            InputStream inputStream = getActivity().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }

            if (bitmap == null) {
                Toast.makeText(getActivity(), "Failed to load image", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create barcode detector if not already created
            BarcodeDetector detector = new BarcodeDetector.Builder(getActivity())
                    .setBarcodeFormats(Barcode.QR_CODE)
                    .build();

            if (!detector.isOperational()) {
                Toast.makeText(getActivity(), "QR detector not available", Toast.LENGTH_SHORT).show();
                detector.release();
                return;
            }

            // Create frame from bitmap and detect
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<Barcode> barcodes = detector.detect(frame);

            if (barcodes.size() > 0) {
                String barcodeValue = barcodes.valueAt(0).displayValue;
                if (barcodeValue != null && !barcodeValue.isEmpty()) {
                    intentData = barcodeValue;
                    scanProcessed = true;
                    onScanSuccess();
                } else {
                    Toast.makeText(getActivity(), "QR code is empty", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getActivity(), "No QR code found in image", Toast.LENGTH_SHORT).show();
            }

            detector.release();
            bitmap.recycle();

        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraSource != null) {
            cameraSource.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        scanProcessed = false; // Reset flag when resuming
        initialiseDetectorsAndSources();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
        if (barcodeDetector != null) {
            barcodeDetector.release();
        }
    }
}
