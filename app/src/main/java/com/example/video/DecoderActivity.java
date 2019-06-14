package com.example.video;

import android.graphics.Bitmap;

import android.graphics.BitmapFactory;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;


import com.example.openglesv3.R;


public class DecoderActivity extends AppCompatActivity {

    private ImageView imageView;
    private VideoDecoder videoDecoder;


    /**
     * 跳帧类
     * 遍历获取视频图片，图片处理时跳帧
     * **/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        imageView = findViewById(R.id.image_view);
        //初始化
        videoDecoder = new VideoDecoder();
        videoDecoder.setOnDecoder(new VideoDecoder.onDecoder() {
            //返回数据
            @Override
            public void onImage(final byte[] data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
                        imageView.setImageBitmap(bitmap);
                        //数据处理完成后取当前解码帧
                        videoDecoder.computeEnd();
                    }
                });
            }
        });
    }


    public void play(View view){
        //设置路径,开始遍历
        videoDecoder.initVideo("/storage/emulated/0/360/test10086.mp4");
        videoDecoder.start();
    }
}
