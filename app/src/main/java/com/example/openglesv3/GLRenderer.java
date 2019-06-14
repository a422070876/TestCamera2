package com.example.openglesv3;

import android.content.Context;
import android.opengl.GLES20;

import com.example.openglesv3.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by 海米 on 2017/8/16.
 */

public class GLRenderer {
    private Context context;

    private FloatBuffer vertexBuffer;
    private FloatBuffer textureVertexBuffer;

    private int programId;


    private int aPositionHandle;
    private int uTextureSamplerHandle;
    private int aTextureCoordHandle;





    public GLRenderer(Context context){
        this.context = context;
        final float[] vertexData = {
                1f, -1f, 0f,
                -1f, -1f, 0f,
                1f, 1f, 0f,
                -1f, 1f, 0f
        };


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

    public void initShader(){
        String vertexShader = ShaderUtils.readRawTextFile(context, R.raw.bitmap_vertext_shader);
        String fragmentShader = ShaderUtils.readRawTextFile(context, R.raw.bitmap_fragment_sharder);

        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);
        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition");
        uTextureSamplerHandle = GLES20.glGetUniformLocation(programId, "sTexture");
        aTextureCoordHandle = GLES20.glGetAttribLocation(programId, "aTexCoord");
        GLES20.glUseProgram(programId);
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false,
                12, vertexBuffer);

        textureVertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTextureCoordHandle);
        GLES20.glVertexAttribPointer(aTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 8, textureVertexBuffer);
    }
    public void drawFrame(){
        GLES20.glUseProgram(programId);
        GLES20.glUniform1i(uTextureSamplerHandle, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    public void release(){
        GLES20.glDeleteProgram(programId);
    }

}
