package com.example.video;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;



/**
 * Created by 海米 on 2017/10/16.
 */

public class VideoRetriever {
    private MediaMetadataRetriever mediaMetadataRetriever;
    //返回视频长度，单位毫秒
    public long initVideo(String path){
        mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(path);

        String time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Long.valueOf(time);
    }
    //取当前时间帧，单位微秒
    public Bitmap getBitmap(long time) {
        return mediaMetadataRetriever.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST);
    }
}
