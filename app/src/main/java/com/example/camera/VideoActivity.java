package com.example.camera;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.openglesv3.R;
import com.example.video.VideoRetriever;

import java.io.File;

public class VideoActivity extends AppCompatActivity {

    private Camera2SurfaceView surfaceView;


    /***
     * 录制视频
     * Camera2SurfaceView直接放到布局文件就可以用
     * **/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.camera_view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //必须,当画面显示开启摄像头
        surfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //必须，当画面被隐藏时录像结束
        surfaceView.onPause();
    }
    public void play(View view){
        Button button = (Button) view;
        if("play".equals(button.getText().toString())){
            //开始录制视频
            File f = new File("/storage/emulated/0/360/test10086.mp4");
            if(f.exists()){
                f.delete();
            }
            surfaceView.startVideo(f);
            button.setText("stop");
        }else if("stop".equals(button.getText().toString())){
            surfaceView.stopVideo();
            button.setText("play");
        }
    }
}
