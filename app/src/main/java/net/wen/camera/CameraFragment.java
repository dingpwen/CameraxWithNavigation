package net.wen.camera;

import android.annotation.SuppressLint;
import android.media.Image;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class CameraFragment extends Fragment {
    private static final String TAG = "CameraFragment";
    private static final double RATIO_4_3_VALUE = 4.0 / 3.0;
    private static final double RATIO_16_9_VALUE = 16.0 / 9.0;

    private PreviewView viewFinder;
    private int displayId;
    private ProcessCameraProvider cameraProvider;
    private int lensFacing;
    private Preview preview;
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;

    @Override
    public void onResume() {
        super.onResume();
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                    .navigate(R.id.action_camera_to_permissions);
        }
        Log.d(TAG, "onResume");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewFinder = view.findViewById(R.id.view_finder);
        cameraExecutor = Executors.newSingleThreadScheduledExecutor();
        viewFinder.post(() -> {
            displayId = viewFinder.getDisplay().getDisplayId();
            // Set up the camera and its use cases
            setUpCamera();
        });
    }

    private void setUpCamera(){
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                if(hasBackCamera()) {
                    lensFacing = CameraSelector.LENS_FACING_BACK;
                } else if(hasFrontCamera()) {
                    lensFacing = CameraSelector.LENS_FACING_FRONT;
                } else {
                    throw new IllegalStateException("Back and front camera are unavailable");
                }
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases() {
        DisplayMetrics metrics = new DisplayMetrics();
        viewFinder.getDisplay().getMetrics(metrics);
        Log.d(TAG, "Screen metrics: "+ metrics.widthPixels + " x " +metrics.heightPixels);

        int screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels);
        Log.d(TAG, "Preview aspect ratio: " + screenAspectRatio);

        int rotation = viewFinder.getDisplay().getRotation();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
        preview = new Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build();

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build();

        imageAnalysis = new ImageAnalysis.Builder()
                .setImageQueueDepth(10)
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            @SuppressLint("UnsafeExperimentalUsageError")
            public void analyze(@NonNull ImageProxy imageProxy) {
                final Image image = imageProxy.getImage();
                if(image != null) {
                    Log.d(TAG, "Image width:" + image.getWidth() + " height:" + image.getHeight() + " format:" + image.getFormat());
                }
                imageProxy.close();//Important, not ignore
            }
        });
        cameraProvider.unbindAll();
        try {
            Camera camera = cameraProvider.bindToLifecycle(this,
                    cameraSelector, preview, imageCapture, imageAnalysis);
            preview.setSurfaceProvider(viewFinder.createSurfaceProvider(camera.getCameraInfo()));
        } catch (Exception exc) {
            Log.e(TAG, "Use case binding failed", exc);
        }
    }

    private int aspectRatio(int width, int height){
        double previewRatio = (double)Math.max(width, height) / (double)Math.min(width, height);
        if (Math.abs(previewRatio - RATIO_4_3_VALUE) <= Math.abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    private boolean hasBackCamera() {
        try {
            return (cameraProvider != null) && cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA);
        } catch (CameraInfoUnavailableException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean hasFrontCamera() {
        try {
            return (cameraProvider != null) && cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA);
        } catch (CameraInfoUnavailableException e) {
            e.printStackTrace();
        }
        return false;
    }
}
