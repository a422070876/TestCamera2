package com.example.video;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by 海米 on 2017/10/16.
 */

public class VideoDecoder {
    private static final String VIDEO = "video/";
    private MediaExtractor mediaExtractor;
    private MediaCodec videoMediaCodec;

    public void initVideo(String filePath){
        try {
            mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(filePath);

            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                MediaFormat format = mediaExtractor.getTrackFormat(i);

                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith(VIDEO)) {
                    mediaExtractor.selectTrack(i);
                    format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                            ImageFormat.YUV_420_888);

                    videoMediaCodec = MediaCodec.createDecoderByType(mime);

                    videoMediaCodec.configure(format, null, null, 0 /* Decoder */);

                    videoMediaCodec.start();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void start(){
        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                videoDecoder();
            }
        };
        thread.start();
    }



    private boolean stop = true;
    public void stop(){
        stop = false;
    }


    private boolean computing = false;
    public void computeEnd(){
        computing = false;
    }

    public void videoDecoder() {
        MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
        while (stop) {
            int inputBufferId = videoMediaCodec.dequeueInputBuffer(1000);
            if (inputBufferId >= 0) {
                ByteBuffer inputBuffer = videoMediaCodec.getInputBuffer(inputBufferId);
                int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
                if (sampleSize < 0) {
                    videoMediaCodec.queueInputBuffer(inputBufferId, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    stop = false;
                } else {
                    long presentationTimeUs = mediaExtractor.getSampleTime();
                    videoMediaCodec.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0);
                    mediaExtractor.advance();
                }
            }
            int outputBufferId = videoMediaCodec.dequeueOutputBuffer(mBufferInfo, 1000);
            if (outputBufferId  >= 0) {
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mBufferInfo.size = 0;
                }
                if (mBufferInfo.size != 0) {
                    Image image = videoMediaCodec.getOutputImage(outputBufferId);
                    if(image != null){
                        if (!computing) {
                            computing = true;
                            byte[] b = VideoToImage.imageToByteArray(image);
                            if(decoder != null){
                                decoder.onImage(b);
                            }
                        }
                        image.close();
                    }

                }

                videoMediaCodec.releaseOutputBuffer(outputBufferId, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;      // out of while
                }
            }
        }
        release();
    }
    public void release() {

        if (videoMediaCodec != null) {
            videoMediaCodec.stop();
            videoMediaCodec.release();
            videoMediaCodec = null;
        }
        if(mediaExtractor != null){
            mediaExtractor.release();
            mediaExtractor = null;
        }
    }

    private onDecoder decoder;

    public void setOnDecoder(onDecoder decoder) {
        this.decoder = decoder;
    }

    public interface onDecoder{
        void onImage(byte[] data);
    }
}
