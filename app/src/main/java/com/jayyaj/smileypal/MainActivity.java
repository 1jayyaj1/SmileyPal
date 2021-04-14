package com.jayyaj.smileypal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import com.jayyaj.smileypal.adapter.FaceDetectionRecyclerAdapter;
import com.jayyaj.smileypal.model.FaceDetection;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity implements FrameProcessor {
    private static final String TAG = "MainActivity";
    private static final int GALLERY_REQUEST_CODE = 10;
    private Facing cameraFacing = Facing.FRONT;
    private ImageView imageView;
    private CameraView faceDetectionCameraView;
    private RecyclerView bottomSheetRecyclerView;
    private BottomSheetBehavior bottomSheetBehavior;
    private ArrayList<FaceDetection> faceDetectionList;
    private Button toggle;
    private FrameLayout bottomSheetButton;
    FaceDetectorOptions highAccuracyOpts;
    FaceDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();

        detector = com.google.mlkit.vision.face.FaceDetection.getClient(highAccuracyOpts);

        faceDetectionList = new ArrayList<>();
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheet));
        imageView = findViewById(R.id.faceDetectionImageView);
        faceDetectionCameraView = findViewById(R.id.faceDetectionCameraView);
        toggle = findViewById(R.id.faceDetectionCameraToggle);
        bottomSheetButton = findViewById(R.id.bottomSheetButton);
        bottomSheetRecyclerView = findViewById(R.id.bottomSheetRecyclerView);
        faceDetectionCameraView.setFacing(cameraFacing);
        faceDetectionCameraView.setLifecycleOwner(MainActivity.this);
        faceDetectionCameraView.addFrameProcessor(MainActivity.this);

        faceDetectionCameraView.addFrameProcessor(frame -> {
            process(frame);
        });

        bottomSheetButton.setOnClickListener(v -> {
            Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
        });

        bottomSheetRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        bottomSheetRecyclerView.setAdapter(new FaceDetectionRecyclerAdapter(MainActivity.this, faceDetectionList));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST_CODE) {

            if (resultCode == RESULT_OK) {
                assert data != null;
                Uri imageUri = data.getData();

                InputImage inputImage;
                try {
                    inputImage = InputImage.fromFilePath(MainActivity.this, imageUri);
                    processImage(inputImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void processImage(InputImage inputImage) {
        if (inputImage == null) {
            Toast.makeText(MainActivity.this, "Image can't be processed", Toast.LENGTH_SHORT).show();
            return;
        }
        imageView.setImageBitmap(null);
        faceDetectionList.clear();

        Objects.requireNonNull(bottomSheetRecyclerView.getAdapter()).notifyDataSetChanged();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        processingStart();

        Task<List<Face>> result =
                detector.process(inputImage)
                        .addOnSuccessListener(
                                faces -> {
                                    // Task completed successfully
                                    processingStop();
                                    for (Face face : faces) {
                                        Rect bounds = face.getBoundingBox();
                                        float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                        float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

                                        // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                                        // nose available):
                                        FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
                                        if (leftEar != null) {
                                            PointF leftEarPos = leftEar.getPosition();
                                        }

                                        // If contour detection was enabled:
                                        List<PointF> leftEyeContour =
                                                face.getContour(FaceContour.LEFT_EYE).getPoints();
                                        List<PointF> upperLipBottomContour =
                                                face.getContour(FaceContour.UPPER_LIP_BOTTOM).getPoints();

                                        // If classification was enabled:
                                        if (face.getSmilingProbability() != null) {
                                            float smileProb = face.getSmilingProbability();
                                            Log.d(TAG, "Smiling probability: " + smileProb);
                                        }
                                        if (face.getRightEyeOpenProbability() != null) {
                                            float rightEyeOpenProb = face.getRightEyeOpenProbability();
                                            Log.d(TAG, "Right eye open probability: " + rightEyeOpenProb);
                                        }

                                        // If face tracking was enabled:
                                        if (face.getTrackingId() != null) {
                                            int id = face.getTrackingId();
                                        }
                                    }
                                })
                        .addOnFailureListener(
                                e -> {
                                    // Task failed with an exception
                                    processingStop();
                                });

    }

    private void processingStart() {
        findViewById(R.id.bottomSheetButtonImage).setVisibility(View.GONE);
        findViewById(R.id.bottomSheetButtonProgressBar).setVisibility(View.VISIBLE);
    }

    private void processingStop() {
        findViewById(R.id.bottomSheetButtonProgressBar).setVisibility(View.GONE);
        findViewById(R.id.bottomSheetButtonImage).setVisibility(View.VISIBLE);
    }

    @Override
    public void process(@NonNull Frame frame) {
        try {
            processingStart();
            int w = frame.getSize().getWidth();
            int h = frame.getSize().getHeight();
            int format = frame.getFormat();
            int ori = frame.getRotationToView();
            byte[] pl = frame.getData();
            Task<List<Face>> result =
                    detector.process(InputImage.fromByteArray(pl, w, h, ori, format))
                            .addOnSuccessListener(
                                    faces -> {
                                        // Task completed successfully
                                        for (Face face : faces) {
                                            Rect bounds = face.getBoundingBox();
                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

                                            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                                            // nose available):
                                            FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
                                            if (leftEar != null) {
                                                PointF leftEarPos = leftEar.getPosition();
                                            }

                                            // If contour detection was enabled:
                                            List<PointF> leftEyeContour =
                                                    face.getContour(FaceContour.LEFT_EYE).getPoints();
                                            List<PointF> upperLipBottomContour =
                                                    face.getContour(FaceContour.UPPER_LIP_BOTTOM).getPoints();

                                            // If classification was enabled:
                                            if (face.getSmilingProbability() != null) {
                                                float smileProb = face.getSmilingProbability();
                                                Log.d(TAG, "Smiling probability: " + smileProb);
                                            }
                                            if (face.getRightEyeOpenProbability() != null) {
                                                float rightEyeOpenProb = face.getRightEyeOpenProbability();
                                                Log.d(TAG, "Right eye open probability: " + rightEyeOpenProb);
                                            }

                                            // If face tracking was enabled:
                                            if (face.getTrackingId() != null) {
                                                int id = face.getTrackingId();
                                            }
                                        }
                                    })
                            .addOnFailureListener(
                                    e -> {
                                        // Task failed with an exception
                                        Log.e(TAG, String.valueOf(e));
                                    });

        }
        catch (Exception e) {
            // Firebase task failed.
            Log.e(TAG, String.valueOf(e));
        }
    }
}