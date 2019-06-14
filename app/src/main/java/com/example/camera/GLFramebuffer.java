package com.example.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.view.Surface;

import com.example.camera.ShaderUtils;
import com.example.openglesv3.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by 海米 on 2017/8/16.
 */

public class GLFramebuffer {
    private Context context;

    private final float[] vertexData = {
            1f, -1f, 0f,
            -1f, -1f, 0f,
            1f, 1f, 0f,
            -1f, 1f, 0f
    };
    private FloatBuffer vertexBuffer;

    private FloatBuffer textureVertexBuffer;

    private int programId;
    private int aPositionHandle;
    private int uTextureSamplerHandle;
    private int aTextureCoordHandle;
    private int uSTMMatrixHandle;

    private float[] mSTMatrix = new float[16];

    private int[] textures;

    private int[] frameBuffers;

    private int[] vertexBuffers;

    private SurfaceTexture surfaceTexture;

    public GLFramebuffer(Context context){
        this.context = context;

        final float[] textureVertexData = {
                1f, 0f,
                0f, 0f,
                1f, 1f,
                0f, 1f
        };
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        textureVertexBuffer = ByteBuffer.allocateDirect(textureVertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureVertexData);
        textureVertexBuffer.position(0);
    }
    public void initFramebuffer(int width,int height){
        String vertexShader = ShaderUtils.readRawTextFile(context, R.raw.vertext_shader);
        String fragmentShader = ShaderUtils.readRawTextFile(context, R.raw.fragment_sharder);

        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);
        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition");
        uSTMMatrixHandle = GLES20.glGetUniformLocation(programId, "uSTMatrix");
        uTextureSamplerHandle = GLES20.glGetUniformLocation(programId, "sTexture");
        aTextureCoordHandle = GLES20.glGetAttribLocation(programId, "aTexCoord");

        vertexBuffers = new int[1];
        GLES20.glGenBuffers(1,vertexBuffers,0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBuffers[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.length*4, vertexBuffer,GLES20.GL_STATIC_DRAW);


        frameBuffers = new int[1];
        GLES20.glGenFramebuffers(1, frameBuffers, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[0]);


        textures = new int[2];
        GLES20.glGenTextures(2, textures, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textures[0], 0);


        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[1]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);


        GLES20.glUseProgram(programId);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBuffers[0]);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false,
                12, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glEnableVertexAttribArray(aTextureCoordHandle);
        GLES20.glVertexAttribPointer(aTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 8, textureVertexBuffer);


        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);


    }

    public SurfaceTexture getSurfaceTexture(){
        surfaceTexture = new SurfaceTexture(textures[1]);
        return surfaceTexture;
    }

    public void drawFrameBuffer(int width,int height,int rotation){
        Matrix.setIdentityM(mSTMatrix,0);
        surfaceTexture.updateTexImage();
        surfaceTexture.getTransformMatrix(mSTMatrix);
        switch (rotation) {
            case Surface.ROTATION_0:
                break;
            case Surface.ROTATION_90:
                Matrix.rotateM(mSTMatrix,0,-90,0,0,1);//根据z轴，以左下角为圆点旋转，角度顺时针为正，逆时针为负
                Matrix.translateM(mSTMatrix,0,-1,0,0);//x轴正向左移，负向右移，y轴正向下移,负向上移
                break;
            case Surface.ROTATION_180:
                break;
            case Surface.ROTATION_270:
                Matrix.rotateM(mSTMatrix,0,90,0,0,1);
                Matrix.translateM(mSTMatrix,0,0,-1,0);
                break;
        }

        GLES20.glUseProgram(programId);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[1]);
        GLES20.glUniform1i(uTextureSamplerHandle,1);
        GLES20.glUniformMatrix4fv(uSTMMatrixHandle, 1, false, mSTMatrix, 0);


        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[0]);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glViewport(0, 0, width, height);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void drawFrame(){
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
    }

    public void release(){
        GLES20.glDeleteTextures(2, textures, 0);
        GLES20.glDeleteBuffers(1,vertexBuffers,0);
        GLES20.glDeleteFramebuffers(1, frameBuffers, 0);
        GLES20.glDeleteProgram(programId);
    }

}
