package com.example.video;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.example.openglesv3.R;


public class RetrieverActivity extends AppCompatActivity {

    private ImageView imageView;
    private VideoRetriever videoRetriever;
    private long time;
    private int index = 0;

    /**
     * 根据时间提取当前帧
     * **/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        imageView = findViewById(R.id.image_view);
        //初始化，设置视频路径
        videoRetriever = new VideoRetriever();
        time = videoRetriever.initVideo("/storage/emulated/0/360/test10086.mp4");
    }


    public void play(View view){
        index++;
        //提取当前时间帧
        Bitmap bitmap = videoRetriever.getBitmap(index*1000); //毫秒转微秒
        imageView.setImageBitmap(bitmap);
        if(index >= time){
            index = 0;
        }
    }
}
