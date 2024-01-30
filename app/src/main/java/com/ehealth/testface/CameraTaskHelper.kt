package com.ehealth.testface


import android.app.Activity
import android.graphics.*
import android.hardware.Camera
import android.util.Log
import android.view.Surface
import android.view.TextureView

import java.util.*


/**
 * author :  chensen
 * data  :  2018/3/17
 * desc :
 */
class CameraTaskHelper(activity: Activity, surfaceView: TextureView) : Camera.PreviewCallback {
    private var mCamera: Camera? = null                   //Camera对象
    private lateinit var mParameters: Camera.Parameters   //Camera对象的参数
    private var mSurfaceView: TextureView = surfaceView   //用于预览的SurfaceView对象

    private var mActivity: Activity = activity
    private var mCallBack: CallBack? = null   //自定义的回调

    var mCameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT //摄像头方向
    var mDisplayOrientation: Int = 0    //预览旋转的角度

    private var mCameraId = 0

    private var picWidth = 640        //保存图片的宽
    private var picHeight = 480       //保存图片的高

    init {
        //初始化相机
        init()
    }


    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
//        mCallBack?.onPreviewFrame(data)
    }

    fun takePic() {
        mCamera?.let {
            it.takePicture({}, null, { data, _ ->
                mCallBack?.onTakePic(data)
            })
        }
    }

    private fun init() {
        if (mSurfaceView.isAvailable) {
            if (mCamera == null) {
                //打开相机
                openCamera(mCameraFacing)
            }
            //开启预览
            startPreview()
        }
        mSurfaceView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                releaseCamera()
                return true
            }

            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                if (mCamera == null) {
                    //打开相机
                    openCamera(mCameraFacing)
                }
                //开启预览
                startPreview()
            }
        }


    }

    //打开相机
    private fun openCamera(cameraFacing: Int = Camera.CameraInfo.CAMERA_FACING_FRONT): Boolean {
        //相机数量为2则打开1,1则打开0,相机ID 1为前置，0为后置
        mCameraId = Camera.getNumberOfCameras() - 1
        //若指定了相机ID且该相机存在，则打开指定的相机
        if (cameraFacing <= mCameraId) {
            mCameraId = cameraFacing
        }
        //没有相机
        if (mCameraId == -1) {
//            toast({ "打开相机失败!" })
            return false
        }
        mCameraFacing = mCameraId
        //如果支持就打开摄像头
        try {
            mCamera = Camera.open(mCameraFacing)
            //初始化摄像头参数
            initParameters(mCamera!!)
            //设置预览回调
            mCamera?.setPreviewCallback(this)
        } catch (e: Exception) {
            e.printStackTrace()
//            toast({ "打开相机失败!" })
            return false
        }
        return true
    }

    //配置相机参数
    private fun initParameters(camera: Camera) {
        try {
            //获取相机配置参数
            mParameters = camera.parameters
            //设置预览格式
            mParameters.previewFormat = ImageFormat.NV21

            //获取与指定宽高相等或最接近的尺寸
            //设置预览尺寸
            val bestPreviewSize = getBestSize(
                mSurfaceView.width,
                mSurfaceView.height,
                mParameters.supportedPreviewSizes
            )
            bestPreviewSize?.let {
                mParameters.setPreviewSize(it.width, it.height)
            }
            //设置保存图片尺寸
            val bestPicSize = getBestSize(picWidth, picHeight, mParameters.supportedPictureSizes)
            bestPicSize?.let {
                mParameters.setPictureSize(it.width, it.height)
            }
            //对焦模式
            if (isSupportFocus(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
                mParameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE

            mParameters.pictureFormat = ImageFormat.NV21
            camera.parameters = mParameters
        } catch (e: Exception) {
            e.printStackTrace()
//            toast { "相机初始化失败!" }
        }
    }

    //开始预览
    fun startPreview() {
        mCamera?.let {
            it.setPreviewTexture(mSurfaceView.surfaceTexture)
            setCameraDisplayOrientation(mActivity)
            it.startPreview()
            it.setOneShotPreviewCallback(object : Camera.PreviewCallback {
                override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
                    mCallBack?.oneFrame(camera!!)
                }
            })
            try {
                it.startFaceDetection()
                it.setFaceDetectionListener(object : Camera.FaceDetectionListener {
                    override fun onFaceDetection(faces: Array<Camera.Face>, camera: Camera?) {
                        mCallBack?.onFaceDetect(transForm(faces))
                    }
                })
            } catch (e: Exception) {
                //说明摄像头不支持人脸识别，就直接不去控制UI
                mCallBack?.onFaceDetect(arrayListOf(RectF(0F, 0F, 0F, 0F)))
            }

        }
    }


    //判断是否支持某一对焦模式
    private fun isSupportFocus(focusMode: String): Boolean {
        var autoFocus = false
        val listFocusMode = mParameters.supportedFocusModes
        for (mode in listFocusMode) {
            if (mode == focusMode)
                autoFocus = true
        }
        return autoFocus
    }


    //释放相机
    fun releaseCamera() {
        if (mCamera != null) {
            mCamera?.stopPreview()
            mCamera?.setPreviewCallback(null)
            mCamera?.release()
            mCamera = null
        }
    }

    //获取与指定宽高相等或最接近的尺寸
    private fun getBestSize(
        targetWidth: Int,
        targetHeight: Int,
        sizeList: List<Camera.Size>
    ): Camera.Size? {
        var bestSize: Camera.Size? = null
        val targetRatio = (targetWidth.toDouble() / targetHeight)  //目标大小的宽高比
        var minDiff = targetRatio

        for (size in sizeList) {
            val supportedRatio = (size.width.toDouble() / size.height)
            Log.e("Camera", "系统支持的尺寸 : ${size.width} * ${size.height} , 比例$supportedRatio")
        }

        for (size in sizeList) {
            if (size.width == targetWidth && size.height == targetHeight) {
                bestSize = size
                break
            }

            val supportedRatio = (size.width.toDouble() / size.height)
            if (Math.abs(supportedRatio - targetRatio) <= minDiff) {
                minDiff = Math.abs(supportedRatio - targetRatio)
                if (size.width > targetWidth) {
                    bestSize = size
                }
            }
        }
        Log.e("Camera", "目标尺寸 ：$targetWidth * $targetHeight ，   比例  $targetRatio")
        Log.e("Camera", "最优尺寸 ：${bestSize?.height} * ${bestSize?.width}")
        return bestSize
    }

    //设置预览旋转的角度
    private fun setCameraDisplayOrientation(activity: Activity) {
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(mCameraFacing, info)
        val rotation = activity.windowManager.defaultDisplay.rotation

        var screenDegree = 0
        when (rotation) {
            Surface.ROTATION_0 -> screenDegree = 0
            Surface.ROTATION_90 -> screenDegree = 90
            Surface.ROTATION_180 -> screenDegree = 180
            Surface.ROTATION_270 -> screenDegree = 270
        }

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mDisplayOrientation = (info.orientation + screenDegree) % 360
            mDisplayOrientation =
                (360 - mDisplayOrientation) % 360          // compensate the mirror
        } else {
            mDisplayOrientation = (info.orientation - screenDegree + 360) % 360
        }
        mCamera?.setDisplayOrientation(mDisplayOrientation)

        Log.e("Camera", "屏幕的旋转角度 : $rotation")

        Log.e("Camera", "setDisplayOrientation(result) : $mDisplayOrientation")
    }


    //将相机中用于表示人脸矩形的坐标转换成UI页面的坐标
    private fun transForm(faces: Array<Camera.Face>): ArrayList<RectF> {
        val matrix = Matrix()
        // Need mirror for front camera.
        val mirror = (mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT)
        matrix.setScale(if (mirror) -1f else 1f, 1f)
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(mDisplayOrientation.toFloat())
        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
        matrix.postScale(mSurfaceView.width / 2000f, mSurfaceView.height / 2000f)
        matrix.postTranslate(mSurfaceView.width / 2f, mSurfaceView.height / 2f)

        val rectList = ArrayList<RectF>()
        for (face in faces) {
            val srcRect = RectF(face.rect)
            val dstRect = RectF(0f, 0f, 0f, 0f)
            matrix.mapRect(dstRect, srcRect)
            rectList.add(dstRect)
        }
        return rectList
    }


    fun getCamera(): Camera? = mCamera

    fun addCallBack(callBack: CallBack) {
        this.mCallBack = callBack
    }


    interface CallBack {
        fun onTakePic(data: ByteArray?)
        fun oneFrame(camera:Camera)
        fun onFaceDetect(faces: ArrayList<RectF>)
    }


}