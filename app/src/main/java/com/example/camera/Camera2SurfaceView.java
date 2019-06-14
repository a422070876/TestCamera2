package com.example.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.content.PermissionChecker;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.File;
import java.util.Arrays;

/**
 * Created by 海米 on 2017/9/5.
 */

public class Camera2SurfaceView extends SurfaceView {

    private SurfaceHolder mHolder;

    private EGLUtils mEglUtils;

    private GLFramebuffer mFramebuffer;
    private GLRenderer mRenderer;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;


    private final Object mCameraObject = new Object();

    private String mCameraId;
    private CameraManager mCameraManager;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder builder;

    private HandlerThread mCameraHandlerThread;
    private Handler mCameraHandler;

    private int screenWidth, screenHeight;

    private int previewWidth, previewHeight;

    public Camera2SurfaceView(Context context) {
        super(context);
        init(context);
    }

    public Camera2SurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {

        mHolder = getHolder();
        mFramebuffer = new GLFramebuffer(context);
        mRenderer = new GLRenderer(context);
        initCamera2();
        mHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int w, int h) {
                screenWidth = w;
                screenHeight = h;
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        super.run();

                        mEglUtils = new EGLUtils();
                        mEglUtils.initWindowEGL(EGL14.EGL_NO_CONTEXT, mHolder.getSurface());
                        mRenderer.initShader();


                        Size mPreviewSize = getPreferredPreviewSize(mSizes, 1280, 720);
                        previewWidth = mPreviewSize.getWidth();
                        previewHeight = mPreviewSize.getHeight();

                        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                        int rotation = windowManager.getDefaultDisplay().getRotation();

                        int left = 0, top = 0, viewWidth = 0, viewHeight = 0;
                        switch (rotation) {
                            case Surface.ROTATION_0:
                                left = 0;
                                viewWidth = screenWidth;
                                viewHeight = (int) (previewWidth * 1.0f / previewHeight * viewWidth);
                                top = (screenHeight - viewHeight) / 2;
                                break;
                            case Surface.ROTATION_90:
                                left = 0;
                                viewWidth = screenWidth;
                                viewHeight = (int) (previewHeight * 1.0f / previewWidth * viewWidth);
                                top = (screenHeight - viewHeight) / 2;
                                break;
                            case Surface.ROTATION_180:
                                break;
                            case Surface.ROTATION_270:
                                break;
                        }


                        Rect rect = new Rect();
                        rect.left = left;
                        rect.top = top;
                        rect.right = left + viewWidth;
                        rect.bottom = top + viewHeight;


                        mFramebuffer.initFramebuffer(previewWidth, previewHeight);

                        mSurfaceTexture = mFramebuffer.getSurfaceTexture();
                        mSurfaceTexture.setDefaultBufferSize(previewWidth, previewHeight);

                        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                            @Override
                            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                                synchronized (mCameraObject) {
                                    mCameraObject.notifyAll();
                                }
                            }
                        });
                        openCamera2();
                        while (true) {
                            synchronized (mCameraObject) {
                                try {
                                    mCameraObject.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (mSurfaceTexture == null) {
                                break;
                            }

                            mFramebuffer.drawFrameBuffer(previewWidth, previewHeight, rotation);

                            if(mVideoEncoder != null){
                                synchronized (vObject){
                                    vObject.notifyAll();
                                }
                            }

                            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

                            GLES20.glViewport(rect.left, rect.top, rect.width(), rect.height());

                            mFramebuffer.drawFrame();
                            mRenderer.drawFrame();


                            mEglUtils.swap();

                        }
                        mEglUtils.release();
                        mFramebuffer.release();
                        mRenderer.release();
                    }
                };
                thread.start();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                if (mSurfaceTexture != null) {
                    mSurfaceTexture.release();
                    mSurfaceTexture = null;
                    synchronized (mCameraObject) {
                        mCameraObject.notifyAll();
                    }
                }
            }
        });

    }

    private VideoEncoder mVideoEncoder;
    private final Object vObject = new Object();


    private boolean stop = false;
    //视频保存路径
    public void startVideo(File videoFile){
        if(mVideoEncoder != null){
            return;
        }
        mVideoEncoder = new VideoEncoder();
        mVideoEncoder.setVideoSize(previewWidth,previewHeight);
        mVideoEncoder.initVideo(videoFile);
        stop = false;
        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                GLRenderer renderer = new GLRenderer(getContext());
                EGLUtils eglUtils = new EGLUtils();
                //把MediaCodec生成的Surface给EGL做渲染用
                eglUtils.initWindowEGL(mEglUtils.getContext(),mVideoEncoder.getSurface());
                renderer.initShader();
                //继续死循环
                while (true){
                    synchronized (vObject) {
                        try {
                            vObject.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if(mSurfaceTexture == null){
                        break;
                    }
                    if(stop){
                        break;
                    }

                    GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
                    GLES20.glViewport(0,0,mVideoEncoder.getVideoWidth(),mVideoEncoder.getVideoHeight());
                    mFramebuffer.drawFrame();
                    renderer.drawFrame();
                    //渲染，这样MediaCodec.dequeueOutputBuffer就有数据了
                    eglUtils.swap();
                }
                eglUtils.release();

            }
        };
        thread.start();
        mVideoEncoder.start();
    }

    public void stopVideo(){
        mVideoEncoder.stop();
        stop = true;
        mVideoEncoder = null;
    }





    public void onResume() {
        startThread();
        if(mSurfaceTexture != null){
            openCamera2();
        }
    }





    public void onPause() {
        if(mVideoEncoder != null){
            stopVideo();
        }
        closeCamera();
        stopThread();
    }
    private void closeCamera(){
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.getDevice().close();
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
    }

    private void startThread() {
        mCameraHandlerThread = new HandlerThread("Camera2");
        mCameraHandlerThread.start();
        mCameraHandler = new Handler(mCameraHandlerThread.getLooper());
    }

    private void stopThread(){
        mCameraHandlerThread.quitSafely();
        try {
            mCameraHandlerThread.join();
            mCameraHandlerThread = null;
            mCameraHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private Size[] mSizes;

    private void initCamera2() {
        mCameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] CameraIdList = mCameraManager.getCameraIdList();
            mCameraId = CameraIdList[0];
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map != null) {
                mSizes = map.getOutputSizes(SurfaceTexture.class);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera2() {
        if (PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                mCameraManager.openCamera(mCameraId, stateCallback, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            takePreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {

        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {

        }
    };

    private void takePreview() {
        try {
            mSurface = new Surface(mSurfaceTexture);


            builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(mSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (null == mCameraDevice) return;
                    mCameraCaptureSession = cameraCaptureSession;
                    builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    CaptureRequest previewRequest = builder.build();
                    try {
                        mCameraCaptureSession.setRepeatingRequest(previewRequest, null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, mCameraHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size getPreferredPreviewSize(Size[] sizes, int width, int height) {
        Size s = null;
        for (Size option : sizes) {
            int w = option.getWidth();
            int h = option.getHeight();
            if(width >= height){
                if(s == null){
                    s = option;
                }else{
                    if(w <= width){
                        if(w > s.getWidth()){
                            s = option;
                        }else{
                            int a = Math.abs(height - h) - Math.abs(height - s.getHeight());
                            if(a < 0){
                                s = option;
                            }else if (a == 0 && h < s.getHeight()){
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
                      if(h > s.getHeight()){
                          s = option;
                      }else{
                          int a = Math.abs(height - w) - Math.abs(height - s.getWidth());
                          if(a < 0){
                              s = option;
                          }else if(a == 0 && w < s.getWidth()){
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
        return sizes[0];
    }
}
