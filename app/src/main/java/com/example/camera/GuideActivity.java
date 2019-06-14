package com.example.camera;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.openglesv3.R;


/**
 * Created by 海米 on 2018/6/7.
 */

public class GuideActivity extends BaseActivity {
    @Override
    public String[] getPermissions() {
        return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO};
    }

    @Override
    public void initView() {
        setContentView(R.layout.activity_guide);
    }

    private String types = null;

    @Override
    public void init() {
        Intent intent = new Intent(GuideActivity.this,VideoActivity.class);
        intent.putExtra("types",types);
        startActivity(intent);
        finish();
    }



    @Override
    public void notOpenPermissions(String[] permissions) {
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
//                Intent intent =  new Intent(Settings.ACTION_SETTINGS);
//                startActivity(intent);
                Toast.makeText(this,"请在设置内开启储存权限后重新打开应用",Toast.LENGTH_LONG).show();
//                finish();
                break;
            }
        }
        init();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            View decorView = getWindow().getDecorView();
            int mHideFlags =
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(mHideFlags);
        }else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(!isTaskRoot()){
            finish();
            return;
        }
        super.onCreate(savedInstanceState);
    }

}
