package com.swan.livepush;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.swan.camera.widget.CameraFocusView;
import com.swan.camera.widget.CameraView;
import com.swan.livepush.databinding.ActivityMainBinding;

public class LivePushActivity extends AppCompatActivity implements ConnectListener {

    private ActivityMainBinding binding;
    private CameraView mCameraView;
    private CameraFocusView mFocusView;
    private DefaultVideoPush mVideoPush;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getPermission();


        binding.liveBt.setOnClickListener(v -> startLivePush());
        mCameraView = binding.cameraView;
        mFocusView = binding.cameraFocusView;
        mCameraView.setOnFocusListener(new CameraView.FocusListener() {
            @Override
            public void beginFocus(int x, int y) {
                mFocusView.beginFocus(x, y);
            }

            @Override
            public void endFocus(boolean success) {
                mFocusView.endFocus(true);
            }
        });


    }

    private void startLivePush() {
        Log.e("TAG","开始推流");
        mVideoPush = new DefaultVideoPush(LivePushActivity.this,
            mCameraView.getEglContext(), mCameraView.getTextureId());
        //mVideoPush.initVideo("rtmp://120.24.85.248/myapp/mystream",LivePushActivity.this,Utils.getScreenWidth(LivePushActivity.this),
        //    Utils.getScreenHeight(LivePushActivity.this));

        mVideoPush.initVideo("rtmp://120.24.85.248/myapp/mystream",LivePushActivity.this,
            720/2,
            1280/2);
        mVideoPush.setOnConnectListener(this);
        mVideoPush.startPush();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoPush.stopPush();
    }

    @Override
    public void connectError(int errCode, String errMsg) {
        Log.e("TAG", "connectError: "+errMsg);
    }

    @Override
    public void connectSuccess() {
        Log.e("TAG", "connectSuccess: 可以推流了");

    }

    private void getPermission() {
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
    }
}