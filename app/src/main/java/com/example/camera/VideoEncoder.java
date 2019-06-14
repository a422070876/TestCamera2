package com.example.camera;

import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by 海米 on 2017/8/15.
 */

public class VideoEncoder {
    //视频参数
    private static final String VIDEO_MIME_TYPE = "video/avc";//视频类型
    private static final int BIT_RATE = 128000; //比特率
    private static final int FRAME_RATE = 25; //帧率
    private static final int FI_FRAME_INTERVAL = 5; //关键帧时间
    private int videoWidth = 1280;
    private int videoHeight = 720;

    private MediaMuxer mediaMuxer;
    private MediaCodec videoMediaCodec;

    private Surface surface;

    public void setVideoSize(int width,int height){
        videoWidth = width;
        videoHeight = height;
    }
    //初始化，和录音差不多就MediaFormat初始化参数不同和获取Surface
    public void initVideo(File file){
        try {
            mediaMuxer = new MediaMuxer(file.getPath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (Exception e){
            e.printStackTrace();
        }
        try {
            videoMediaCodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, videoWidth, videoHeight);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, FI_FRAME_INTERVAL);
        videoMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        surface = videoMediaCodec.createInputSurface();
        videoMediaCodec.start();
    }

    public Surface getSurface() {
        return surface;
    }
    public int getVideoWidth(){
        return videoWidth;
    }
    public int getVideoHeight(){
        return videoHeight;
    }

    public void start(){
        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                videoEncoder();
            }
        };
        thread.start();
    }



    private boolean stop = true;
    public void stop(){
        stop = false;
    }
    //继续死循环
    public void videoEncoder() {
        MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
        int mTrackIndex = -1;
        while (stop) {
            //等待数据返回，阻塞线程，时间1000毫秒
            int encoderStatus = videoMediaCodec.dequeueOutputBuffer(mBufferInfo, 1000);
            if (encoderStatus >= 0) {
                ByteBuffer encodedData = videoMediaCodec.getOutputBuffer(encoderStatus);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mBufferInfo.size = 0;
                }
                //有数据是写入
                if (mBufferInfo.size != 0) {
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                    writeData(mTrackIndex, encodedData, mBufferInfo);
                }
                videoMediaCodec.releaseOutputBuffer(encoderStatus, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;      // out of while
                }
            }else if(encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER){
                //负数，听说是一段数据写入完成传入INFO_TRY_AGAIN_LATER，会进入这里，用Surface不知道会不会进入，没验证过
            }else if(encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                MediaFormat mediaFormat = videoMediaCodec.getOutputFormat();
                mTrackIndex = mediaMuxer.addTrack(mediaFormat);
                mediaMuxer.start();
            }else{
                //不是正数都跳过
            }
        }
        //使用Surface需要调用，通知MediaCodec停止接收数据，直接数据操作时不需要调用
        videoMediaCodec.signalEndOfInputStream();

        release();
    }
    //防止多线程时同时调用
    public synchronized void writeData(int mTrackIndex,ByteBuffer encodedData,MediaCodec.BufferInfo mBufferInfo){
        mediaMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
    }
    //释放
    public void release() {
        if(surface != null){
            surface.release();
            surface = null;
        }
        if (videoMediaCodec != null) {
            videoMediaCodec.stop();
            videoMediaCodec.release();
            videoMediaCodec = null;
        }
        if(mediaMuxer != null){
            mediaMuxer.release();
            mediaMuxer = null;
        }
    }
}
