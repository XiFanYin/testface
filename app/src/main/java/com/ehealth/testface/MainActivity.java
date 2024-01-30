package com.ehealth.testface;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.zl.face.FaceDetector;
import com.zl.face.FaceInfo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import static com.ehealth.testface.DpSpUtilsKt.dp2px;

public class MainActivity extends AppCompatActivity {

    private FaceDetector detector;
    private TextureView image;
    private ImageView image2;
    private TextView text;
    private TextureView previewView;

    private CameraTaskHelper mCameraTaskHelper;

    private CameraHelper cameraHelper;

    float[] test;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        image = findViewById(R.id.image);
        image2 = findViewById(R.id.image2);
        text = findViewById(R.id.text);
        previewView = findViewById(R.id.previewView);
        mCameraTaskHelper = new CameraTaskHelper(MainActivity.this, previewView);
        mCameraTaskHelper.addCallBack(new CameraTaskHelper.CallBack() {
            @Override
            public void onFaceDetect(ArrayList<RectF> faces) {

            }

            @Override
            public void onTakePic(byte[] data) {
                Bitmap rawBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                Bitmap cropBitmap = FaceCj.cutFace(rawBitmap, MainActivity.this);
                Matrix matrix = new Matrix();
                matrix.setScale(-1.0f, 1.0f);
                int width = cropBitmap.getWidth();
                int height = cropBitmap.getHeight();
                Bitmap rotateBitmap = Bitmap.createBitmap(cropBitmap, 0, 0, width, height, matrix, true);
                image2.setImageBitmap(rotateBitmap);

                String filePath = getExternalFilesDir("face") + "/face.jpg";
                try {
                    File imageFile = new File(filePath);
                    // 创建输出流对象
                    FileOutputStream fos = new FileOutputStream(imageFile);
                    // 从Bitmap对象中读取图像数据并写入到输出流中
                    rawBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    // 关闭输出流
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                FaceInfo[] faces = detector.detect(filePath);
                if (faces.length == 1) {
                    test = detector.getFeature(filePath);
                    mCameraTaskHelper.releaseCamera();
                }


            }

            @Override
            public void oneFrame(Camera camera) {

            }


        });


        findViewById(R.id.init).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detector = new FaceDetector();
                detector.setThreadNum(1);
                boolean init = detector.init(MainActivity.this, false);

            }
        });


        findViewById(R.id.getimgfa).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraTaskHelper.takePic();
            }
        });


        findViewById(R.id.openCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                cameraHelper = new CameraHelper.Builder()
                        .previewViewSize(new Point(dp2px(199F), dp2px(149F)))//这里数据和布局宽高一直，之所以写死是因为页面加载的时候布局大小还未确定，所以写死
                        .rotation(getWindowManager().getDefaultDisplay().getRotation())
                        .specificCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT)
                        .previewOn(image)
                        .cameraListener(new CameraListener() {
                            @Override
                            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                            }

                            @Override
                            public void onPreview(byte[] data, Camera camera) {

                                ThreadPoolUtil.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (data != null) {
                                            Bitmap rawBitmap = nv21ToBitmap(data, camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height);
                                            String filePath = getExternalFilesDir("face") + "/face.jpg";
                                            try {
                                                File imageFile = new File(filePath);
                                                // 创建输出流对象
                                                FileOutputStream fos = new FileOutputStream(imageFile);
                                                // 从Bitmap对象中读取图像数据并写入到输出流中
                                                if (rawBitmap!=null){
                                                    rawBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                                                }
                                                // 关闭输出流
                                                fos.close();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                            FaceInfo[] faceInfos = detector.detect(filePath);
                                            FaceInfo info = findMaxFace(faceInfos);
                                            if (info != null) {
                                                Log.e("rrrrrrrrrrrrr", info.toString());
                                                float[] current = detector.getFeature(filePath);
                                                double v1 = detector.featureCompare(test, current);
                                                Log.e("rrrrrrrrrrrrrrr", "相似度:" + v1);
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        text.setText("相似度:" + v1);
                                                    }
                                                });


                                            }

                                        }
                                    }
                                });


                            }

                            @Override
                            public void onCameraClosed() {

                            }

                            @Override
                            public void onCameraError(Exception e) {

                            }

                            @Override
                            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {

                            }
                        })
                        .build();
                cameraHelper.init();
                cameraHelper.start();

            }


        });
//
    }

    private static Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
        Bitmap bitmap = null;
        try {
            YuvImage image = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, width, height), 80, stream);
            bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static FaceInfo findMaxFace(FaceInfo[] faceInfos) {

        if (null == faceInfos || faceInfos.length < 1) {
            return null;
        }
        FaceInfo maxFaceInfo = null;
        for (FaceInfo faceInfo : faceInfos) {
            if (maxFaceInfo != null && maxFaceInfo.getFaceRect().width() * maxFaceInfo.getFaceRect().height() > maxFaceInfo.getFaceRect().width() * maxFaceInfo.getFaceRect().height()) {
                break;
            } else {
                maxFaceInfo = faceInfo;
            }
        }
        return maxFaceInfo;
    }


}