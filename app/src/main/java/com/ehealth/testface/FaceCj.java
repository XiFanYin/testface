package com.ehealth.testface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.media.FaceDetector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class FaceCj {
    private static BitmapFactory.Options BitmapFactoryOptionsbfo;

    private static ByteArrayOutputStream out;

    private static byte[] data;

    private static FaceDetector.Face[] myFace;

    private static FaceDetector myFaceDetect;

    private static int tx = 0;

    private static int ty = 0;

    private static int bx = 0;

    private static int by = 0;

    private static int width = 0;

    private static int height = 0;

    private static float wuchax = 0;

    private static float wuchay = 0;

    private static FaceDetector.Face face;

    private static PointF myMidPoint;

    private static float myEyesDistance;

    private static List facePaths;

    private static String facePath;

    public static Bitmap cutFace(Bitmap bitmap, Context context) {
        facePaths = null;
        BitmapFactoryOptionsbfo = new BitmapFactory.Options();
        BitmapFactoryOptionsbfo.inPreferredConfig = Bitmap.Config.RGB_565; //   构造位图生成的参数，必须为565。类名+enum
        out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        data = out.toByteArray();

        bitmap = BitmapFactory.decodeByteArray(data, 0, data.length,
                BitmapFactoryOptionsbfo);

        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        width = bitmap.getWidth();
        height = bitmap.getHeight();
        myFace = new FaceDetector.Face[5]; // 分配人脸数组空间
        myFaceDetect = new FaceDetector(bitmap.getWidth(), bitmap.getHeight(), 5);
        int numberOfFaceDetected = myFaceDetect.findFaces(bitmap, myFace);
        if (numberOfFaceDetected <= 0) {// FaceDetector构造实例并解析人脸
            bitmap.recycle();
            return null;

        }

        facePaths = new ArrayList();

        for (int i = 0; i < numberOfFaceDetected; i++) {
            face = myFace[i];

            myMidPoint = new PointF();

            face.getMidPoint(myMidPoint);

            myEyesDistance = face.eyesDistance();     //得到人脸中心点和眼间距离参数，并对每个人脸进行画框

            wuchax = myEyesDistance / 2 + myEyesDistance;

            wuchay = myEyesDistance * 2 / 3 + myEyesDistance;

            if (myMidPoint.x - wuchax < 0) {//判断左边是否出界
                tx = 0;
            } else {
                tx = (int) (myMidPoint.x - wuchax);
            }

            if (myMidPoint.x + wuchax > width) {//判断右边是否出界
                bx = width;
            } else {
                bx = (int) (myMidPoint.x + wuchax);
            }

            if (myMidPoint.y - wuchay < 0) {//判断上边是否出界
                ty = 0;
            } else {
                ty = (int) (myMidPoint.y - wuchay);
            }

            if (myMidPoint.y + wuchay > height) {//判断下边是否出界
                by = height;
            } else {
                by = (int) (myMidPoint.y + wuchay);
            }
            try {
                return Bitmap.createBitmap(bitmap, tx, ty, bx - tx, by - ty);//这里可以自行调整裁剪宽高

            } catch (Exception e) {
                e.printStackTrace();

            }

        }

        bitmap.recycle();

        return bitmap;

    }

}