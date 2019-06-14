package com.example.openglesv3;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;



import java.io.IOException;
import java.util.List;

/**
 * Created by 海米 on 2017/8/18.
 */

public class CameraSurfaceView extends SurfaceView {

    private SurfaceHolder mHolder;

    private EGLUtils mEglUtils;

    private GLFramebuffer mFramebuffer;
    private GLRenderer mRenderer;
    private SurfaceTexture mSurfaceTexture;

    private final Object mObject = new Object();

    private Camera mCamera;
    private SensorManager mSensorManager;

    private int screenWidth, screenHeight;


    public CameraSurfaceView(Context context) {
        super(context);
        init(context);
    }
    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    private void init(Context context){
        mHolder = getHolder();
        mFramebuffer = new GLFramebuffer(context);
        mRenderer = new GLRenderer(context);
        mHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int w, int h) {
                screenWidth = w;
                screenHeight = h;
                Thread thread = new Thread(){
                    @Override
                    public void run() {
                        super.run();

                        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                        int cameraCount = Camera.getNumberOfCameras();
                        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
                            Camera.getCameraInfo(camIdx, cameraInfo);
                            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                                mCamera = Camera.open(camIdx);
                                break;
                            }
                        }

                        mEglUtils = new EGLUtils();
                        mEglUtils.initWindowEGL(EGL14.EGL_NO_CONTEXT,mHolder.getSurface());

                        mRenderer.initShader();

                        Camera.Parameters parameters = mCamera.getParameters();

                        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
                        Camera.Size mPreviewSize =  getPreferredPreviewSize(sizes,screenWidth,screenHeight);

                        int previewWidth = mPreviewSize.width;
                        int previewHeight = mPreviewSize.height;
                        Log.d("============",previewWidth+"x"+previewHeight);
                        parameters.setPreviewSize(previewWidth,previewHeight);

                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);






                        mFramebuffer.initFramebuffer(previewWidth,previewHeight);

                        mSurfaceTexture = mFramebuffer.getSurfaceTexture();
                        mSurfaceTexture.setDefaultBufferSize(previewWidth, previewHeight);
                        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                            @Override
                            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                                synchronized (mObject) {
                                    mObject.notifyAll();
                                }
                            }
                        });
                        try {
                            mCamera.setPreviewTexture(mSurfaceTexture);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mSensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
                        Sensor mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                        mSensorManager.registerListener(mSensorEventListener,mSensor,SensorManager.SENSOR_DELAY_GAME);
                        mCamera.startPreview();
                        while (true){
                            synchronized (mObject) {
                                try {
                                    mObject.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            if(mSurfaceTexture == null){
                                break;
                            }

                            mFramebuffer.drawFrameBuffer(previewWidth,previewHeight,0);

                            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);


                            mFramebuffer.drawFrame();
                            mRenderer.drawFrame();


                            mEglUtils.swap();
                        }
                        mEglUtils.release();

                    }
                };
                thread.start();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                if (mCamera != null) {
                    mCamera.release();
                    mCamera = null;
                }
                if(mSensorManager != null){
                    mSensorManager.unregisterListener(mSensorEventListener);
                }
            }
        });
    }

    private Camera.Size getPreferredPreviewSize(List<Camera.Size> sizes, int width, int height) {
        Camera.Size s = null;
        for (Camera.Size option : sizes) {
            int w = option.width;
            int h = option.height;
            if(width >= height){
                if(s == null){
                    s = option;
                }else{
                    if(w <= width){
                        if(w > s.width){
                            s = option;
                        }else{
                            int a = Math.abs(height - h) - Math.abs(height - s.height);
                            if(a < 0){
                                s = option;
                            }else if (a == 0 && h < s.height){
                                s = option;
                            }
                        }
                    }
                }
            }else{
                if(s == null){
                    s = option;
                }else{
                    if(h <= width){
                        if(h > s.height){
                            s = option;
                        }else{
                            int a = Math.abs(height - w) - Math.abs(height - s.width);
                            if(a < 0){
                                s = option;
                            }else if(a == 0 && w < s.width){
                                s = option;
                            }
                        }
                    }
                }
            }
        }
        if(s !=null){
            return s;
        }

        return sizes.get(0);
    }
    private float mLastX,mLastY,mLastZ;
    private boolean mInitialized = true,mAutoFocus = true;
    private SensorEventListener mSensorEventListener =  new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            int sensorType = sensorEvent.sensor.getType();
            if(sensorType == Sensor.TYPE_ACCELEROMETER){
                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];
                if (!mInitialized){
                    mLastX = x;
                    mLastY = y;
                    mLastZ = z;
                    mInitialized = true;
                    mCamera.autoFocus(mAutoFocusCallback);
                }else {
                    float deltaX  = Math.abs(mLastX - x);
                    float deltaY = Math.abs(mLastY - y);
                    float deltaZ = Math.abs(mLastZ - z);

                    if (deltaX > .5 && mAutoFocus){
                        mAutoFocus = false;
                        mCamera.autoFocus(mAutoFocusCallback);
                    }
                    if (deltaY > .5 && mAutoFocus){
                        mAutoFocus = false;
                        mCamera.autoFocus(mAutoFocusCallback);
                    }
                    if (deltaZ > .5 && mAutoFocus){
                        mAutoFocus = false;
                        mCamera.autoFocus(mAutoFocusCallback);
                    }
                }
                mLastX = x;
                mLastY = y;
                mLastZ = z;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };
    private Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean b, Camera camera) {
            mAutoFocus = true;
            if(b){
                camera.cancelAutoFocus();
            }
        }
    };
}
