package com.jayyaj.smileypal;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
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
import com.otaliastudios.cameraview.controls.Preview;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements FrameProcessor {
    private static final String TAG = "MainActivity";
    private Facing cameraFacing = Facing.FRONT;
    private ImageView imageView;
    private CameraView faceDetectionCameraView;
    private RecyclerView bottomSheetRecyclerView;
    private BottomSheetBehavior bottomSheetBehavior;
    private ArrayList<FaceDetection> faceDetectionList;
    private FrameLayout faceDetectionCameraToggle;
    FaceDetectorOptions highAccuracyOpts;
    FaceDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        highAccuracyOpts =
//                new FaceDetectorOptions.Builder()
//                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
//                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
//                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
//                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
//                        .build();

        highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();

        detector = com.google.mlkit.vision.face.FaceDetection.getClient(highAccuracyOpts);

        faceDetectionList = new ArrayList<>();

        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheet));
        imageView = findViewById(R.id.faceDetectionImageView);
        faceDetectionCameraView = findViewById(R.id.faceDetectionCameraView);
        faceDetectionCameraToggle = findViewById(R.id.faceDetectionCameraToggle);
        bottomSheetRecyclerView = findViewById(R.id.bottomSheetRecyclerView);
        faceDetectionCameraView.setFacing(cameraFacing);
        faceDetectionCameraView.setLifecycleOwner(MainActivity.this);
        faceDetectionCameraView.addFrameProcessor(MainActivity.this);

        faceDetectionCameraView.addFrameProcessor(frame -> {
            faceDetectionList.clear();
            Objects.requireNonNull(bottomSheetRecyclerView.getAdapter()).notifyDataSetChanged();
            process(frame);
            Objects.requireNonNull(bottomSheetRecyclerView.getAdapter()).notifyDataSetChanged();
        });

        faceDetectionCameraToggle.setOnClickListener(v -> {
            cameraFacing = cameraFacing == Facing.BACK ? Facing.FRONT : Facing.BACK;
            faceDetectionCameraView.setFacing(cameraFacing);
        });

        bottomSheetRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        bottomSheetRecyclerView.setAdapter(new FaceDetectionRecyclerAdapter(MainActivity.this, faceDetectionList));
    }


    @Override
    public void process(@NonNull Frame frame) {
        try {
            int w = frame.getSize().getWidth();
            int h = frame.getSize().getHeight();
            int format = frame.getFormat();
            int ori = frame.getRotationToView();
            byte[] pl = frame.getData();
            Task<List<Face>> result =
                    detector.process(InputImage.fromByteArray(pl, w, h, ori, format))
                            .addOnSuccessListener(
                                    faces -> {

                                        Bitmap bitmap = Bitmap.createBitmap(faceDetectionCameraView.getWidth(), faceDetectionCameraView.getHeight(), Bitmap.Config.ARGB_8888);

                                        imageView.setImageBitmap(null);

                                        Canvas canvas = new Canvas(bitmap);

                                        Paint facePaint = new Paint();
                                        facePaint.setColor(Color.GREEN);
                                        facePaint.setStyle(Paint.Style.STROKE);
                                        facePaint.setStrokeWidth(5f);

                                        Paint faceTextPaint = new Paint();
                                        faceTextPaint.setColor(Color.BLUE);
                                        faceTextPaint.setTextSize(30f);
                                        faceTextPaint.setTypeface(Typeface.SANS_SERIF);

                                        Paint landmarkPaint = new Paint();
                                        landmarkPaint.setColor(Color.RED);
                                        landmarkPaint.setStyle(Paint.Style.FILL);
                                        landmarkPaint.setStrokeWidth(8f);

                                        // Task completed successfully
                                        for (Face face : faces) {
                                            Rect bounds = face.getBoundingBox();
                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

                                            canvas.drawRect(bounds, facePaint);
                                            canvas.drawText("Face" + face.getTrackingId(),
                                                    (bounds.centerX() - (bounds.width() / 2f) + 8f),
                                                    (bounds.centerY() - (bounds.height() / 2f) + 8f),
                                                    faceTextPaint);

                                            FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
                                            if (leftEye != null) {
                                                canvas.drawCircle(leftEye.getPosition().x, leftEye.getPosition().y, 8f, landmarkPaint);
                                            }

                                            FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
                                            if (rightEye != null) {
                                                canvas.drawCircle(rightEye.getPosition().x, rightEye.getPosition().y, 8f, landmarkPaint);
                                            }

                                            FaceLandmark nose = face.getLandmark(FaceLandmark.NOSE_BASE);
                                            if (nose != null) {
                                                canvas.drawCircle(nose.getPosition().x, nose.getPosition().y, 8f, landmarkPaint);
                                            }

                                            FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
                                            if (leftEar != null) {
                                                canvas.drawCircle(leftEar.getPosition().x, leftEar.getPosition().y, 8f, landmarkPaint);
                                            }

                                            FaceLandmark rightEar = face.getLandmark(FaceLandmark.RIGHT_EAR);
                                            if (rightEar != null) {
                                                canvas.drawCircle(rightEar.getPosition().x, rightEar.getPosition().y, 8f, landmarkPaint);
                                            }

                                            FaceLandmark leftCheek = face.getLandmark(FaceLandmark.LEFT_CHEEK);
                                            if (leftCheek != null) {
                                                canvas.drawCircle(leftCheek.getPosition().x, leftCheek.getPosition().y, 8f, landmarkPaint);
                                            }

                                            FaceLandmark rightCheek = face.getLandmark(FaceLandmark.RIGHT_CHEEK);
                                            if (rightCheek != null) {
                                                canvas.drawCircle(rightCheek.getPosition().x, rightCheek.getPosition().y, 8f, landmarkPaint);
                                            }

                                            FaceLandmark mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM);
                                            FaceLandmark mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT);
                                            FaceLandmark mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT);
                                            if (mouthBottom != null && mouthLeft != null && mouthRight != null) {
                                                canvas.drawLine(mouthLeft.getPosition().x, mouthLeft.getPosition().y, mouthBottom.getPosition().x, mouthBottom.getPosition().y, landmarkPaint);
                                                canvas.drawLine(mouthBottom.getPosition().x, mouthBottom.getPosition().y, mouthRight.getPosition().x, mouthRight.getPosition().y, landmarkPaint);
                                            }

                                            faceDetectionList.add(new FaceDetection(1, "ok"));

                                            //Flip image!
                                            Matrix matrix = new Matrix();
                                            matrix.preScale(-1f, 1f);
                                            Bitmap flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                                                    bitmap.getWidth(), bitmap.getHeight(),
                                                    matrix, true);
                                            imageView.setImageBitmap(flippedBitmap);

//                                            if (leftEar != null) {
//                                                PointF leftEarPos = leftEar.getPosition();
//                                            }
//
//                                            // If contour detection was enabled:
//                                            if (face.getContour(FaceContour.LEFT_EYE) != null) {
//                                                List<PointF> leftEyeContour =
//                                                        face.getContour(FaceContour.LEFT_EYE).getPoints();
//                                            }
//                                            if (face.getContour(FaceContour.UPPER_LIP_BOTTOM) != null) {
//                                                List<PointF> upperLipBottomContour =
//                                                        face.getContour(FaceContour.UPPER_LIP_BOTTOM).getPoints();
//                                            }
//
//                                            // If classification was enabled:
//                                            if (face.getSmilingProbability() != null) {
//                                                float smileProb = face.getSmilingProbability();
//                                                Log.d(TAG, "Smiling probability: " + smileProb);
//                                            }
//                                            if (face.getRightEyeOpenProbability() != null) {
//                                                float rightEyeOpenProb = face.getRightEyeOpenProbability();
//                                                Log.d(TAG, "Right eye open probability: " + rightEyeOpenProb);
//                                            }
//
//                                            // If face tracking was enabled:
//                                            if (face.getTrackingId() != null) {
//                                                int id = face.getTrackingId();
//                                            }
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