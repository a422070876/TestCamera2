package com.example.openglesv3;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.view.Surface;


/**
 * Created by 海米 on 2017/8/15.
 */

public class EGLUtils {
    private EGLSurface eglSurface = EGL14.EGL_NO_SURFACE;
    private EGLDisplay eglDisplay= EGL14.EGL_NO_DISPLAY;

    private EGLContext eglContext = EGL14.EGL_NO_CONTEXT;

    public void initWindowEGL(EGLContext eglContext, Surface surface) {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        int[] version = new int[2];
        EGL14.eglInitialize(eglDisplay, version, 0, version, 1);
        int confAttr[] = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_SURFACE_TYPE,
                EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        EGL14.eglChooseConfig(eglDisplay, confAttr, 0, configs, 0, 1, numConfigs, 0);
        int ctxAttr[] = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,// 0x3098
                EGL14.EGL_NONE
        };
        this.eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], eglContext, ctxAttr, 0);
        int[] surfaceAttr = {
                EGL14.EGL_NONE
        };
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, surfaceAttr, 0);

        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, this.eglContext);

    }
    public void initBufferEGL(EGLContext eglContext){
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        int[] version = new int[2];
        EGL14.eglInitialize(eglDisplay, version, 0, version, 1);
        int confAttr[] = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_SURFACE_TYPE,
                EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        EGL14.eglChooseConfig(eglDisplay, confAttr, 0, configs, 0, 1, numConfigs, 0);
        int ctxAttr[] = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,// 0x3098
                EGL14.EGL_NONE
        };
        this.eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], eglContext, ctxAttr, 0);
        int[] surfaceAttr = {
                EGL14.EGL_NONE
        };
        eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, configs[0],  surfaceAttr, 0);

        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, this.eglContext);
    }


    public EGLContext getContext() {
        return eglContext;
    }

    public void swap() {
        EGL14.eglSwapBuffers(eglDisplay, eglSurface);
    }

    public void release() {
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(eglDisplay, eglSurface);
            eglSurface = EGL14.EGL_NO_SURFACE;
        }
        if (eglContext != EGL14.EGL_NO_CONTEXT ) {
            eglContext = EGL14.EGL_NO_CONTEXT;
        }
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglTerminate(eglDisplay);
            eglDisplay = EGL14.EGL_NO_DISPLAY;
        }
    }
}
