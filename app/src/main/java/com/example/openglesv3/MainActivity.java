package com.example.openglesv3;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.video.VideoDecoder;
import com.example.video.VideoRetriever;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
    private String[] denied;
    private String[] permissions = {Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> list = new ArrayList<>();
            for (int i = 0; i < permissions.length;i++){
                if(PermissionChecker.checkSelfPermission(this, permissions[i]) == PackageManager.PERMISSION_DENIED){
                    list.add(permissions[i]);
                }
            }
            if(list.size() != 0){
                denied = new String[list.size()];
                for (int i = 0 ; i < list.size();i++){
                    denied[i] = list.get(i);
                    ActivityCompat.requestPermissions(this, denied, 5);
                }

            }else{
                init();
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 5) {
            boolean isDenied = false;
            for (int i = 0; i < denied.length; i++) {
                String permission = denied[i];
                for (int j = 0; j < permissions.length; j++) {
                    if (permissions[j].equals(permission)) {
                        if (grantResults[j] != PackageManager.PERMISSION_GRANTED) {
//                            if(ActivityCompat.shouldShowRequestPermissionRationale(this, permission)){
//                            }
                            isDenied = true;
                            break;
                        }
                    }
                }
            }
            if (isDenied) {
                Toast.makeText(this, "请开启权限", Toast.LENGTH_SHORT).show();
            } else {
                init();

            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    private ImageView imageView;
    private VideoRetriever videoRetriever;
    private int time;
    private int index = 0;
    private void init() {
        setContentView(R.layout.activity_main);
//        imageView = findViewById(R.id.image_view);
//        videoRetriever = new VideoRetriever();
//        time = videoRetriever.initVideo("/storage/emulated/0/360/test10086.mp4");


        videoDecoder = new VideoDecoder();
        videoDecoder.setOnDecoder(new VideoDecoder.onDecoder() {

            @Override
            public void onImage(final byte[] data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
                        imageView.setImageBitmap(bitmap);
                        videoDecoder.computeEnd();
                    }
                });
            }
        });

    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();


    private VideoDecoder videoDecoder;
    public void play(View view){
//        index++;
//        Bitmap bitmap = videoRetriever.getBitmap(index*1000*1000);
//        imageView.setImageBitmap(bitmap);
//        if(index >= time){
//            index = 0;
//        }


        videoDecoder.initVideo("/storage/emulated/0/360/test10086.mp4");
        videoDecoder.start();

//        Button button = (Button) view;
//        if("play".equals(button.getText().toString())){
//            surfaceView.startVideo();
//            button.setText("stop");
//        }else if("stop".equals(button.getText().toString())){
//            surfaceView.stopVideo();
//            button.setText("play");
//        }
    }
}
