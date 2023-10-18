package com.swan.livepush;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.swan.camera.widget.CameraFocusView;
import com.swan.camera.widget.CameraView;
import com.swan.livepush.databinding.ActivityMainBinding;
import com.swan.opengl.Utils;

public class LivePushActivity extends AppCompatActivity implements ConnectListener {

    private ActivityMainBinding binding;
    private CameraView mCameraView;
    private CameraFocusView mFocusView;
    private DefaultVideoPush mVideoPush;

    private LivePush mLivePush;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mLivePush = new LivePush("rtmp://120.24.85.248/myapp/mystream");
        mLivePush.setOnConnectListener(this);

        binding.liveBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLivePush();
            }
        });
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
        mLivePush.initConnect();
        Log.e("TAG","开始推流");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLivePush.stop();
    }

    @Override
    public void connectError(int errCode, String errMsg) {
        Log.e("TAG", "connectError: "+errMsg);
    }

    @Override
    public void connectSuccess() {
        Log.e("TAG", "connectSuccess: 可以推流了");
        mVideoPush = new DefaultVideoPush(LivePushActivity.this,
            mCameraView.getEglContext(), mCameraView.getTextureId());
        mVideoPush.initVideo(Utils.getScreenWidth(LivePushActivity.this),
            Utils.getScreenHeight(LivePushActivity.this));
        mVideoPush.startPush();
    }
}