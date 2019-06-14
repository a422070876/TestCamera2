package com.example.camera;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 海米 on 2018/10/8.
 */

public abstract class BaseActivity extends AppCompatActivity {
    public static int PermissionStatusNone = 0;
    public static int PermissionStatusAgree = 1;
    public static int PermissionStatusRefuse = 2;
    public static int PermissionStatusRejected = 3;
    private String[] denied;
    private String[] permissions;// = {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO};
    private String[] refusePermissions;
    private int permissionStatus = PermissionStatusNone;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        onPermissionsResult();
    }
    protected void onPermissionsResult(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissions = getPermissions();
            if(permissions != null){
                ArrayList<String> list = new ArrayList<>();
                for (int i = 0; i < permissions.length; i++) {
                    if (PermissionChecker.checkSelfPermission(this, permissions[i]) == PackageManager.PERMISSION_DENIED) {
                        list.add(permissions[i]);
                    }
                }
                if (list.size() != 0) {
                    denied = new String[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        denied[i] = list.get(i);
                    }
                    ActivityCompat.requestPermissions(this, denied, 5);
                } else {
                    permissionStatus = PermissionStatusAgree;
                    init();
                }
            }else{
                permissionStatus = PermissionStatusAgree;
                init();
            }
        } else {
            permissionStatus = PermissionStatusAgree;
            init();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 5) {
            boolean isDenied = false;
            List<String> list = new ArrayList<>();
            for (int i = 0; i < denied.length; i++) {
                String permission = denied[i];
                for (int j = 0; j < permissions.length; j++) {
                    if (permissions[j].equals(permission)) {
                        if (grantResults[j] != PackageManager.PERMISSION_GRANTED) {
                            isDenied = true;
                            list.add(permission);
                        }
                    }
                }
            }
            if (isDenied) {
                refusePermissions = list.toArray(new String[list.size()]);
                permissionStatus = PermissionStatusRefuse;
            } else {
                permissionStatus = PermissionStatusAgree;
                init();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    protected int getPermissionStatus(){
        return permissionStatus;
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(permissionStatus == PermissionStatusRefuse){
            permissionStatus = PermissionStatusRejected;
            notOpenPermissions(refusePermissions);
        }else if(permissionStatus == PermissionStatusRejected){
            onPermissionsResult();
        }
    }


    public abstract void init();

    public abstract  String[] getPermissions();
    public void initView(){

    }
    public void notOpenPermissions(String[] permissions){

    }
    public Activity getActivity(){
        return this;
    }
}
