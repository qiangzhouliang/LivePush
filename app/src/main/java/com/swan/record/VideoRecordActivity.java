package com.swan.record;

import android.Manifest;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.swan.camera.widget.CameraFocusView;
import com.swan.camera.widget.CameraView;
import com.swan.livepush.databinding.ActivityTestBinding;

/**
 * @ClassName VideoRecordActivity
 * @Description
 * @Author swan
 * @Date 2023/10/17 17:23
 **/
public class VideoRecordActivity extends AppCompatActivity {
    private ActivityTestBinding binding;
    private CameraView mCameraView;
    private CameraFocusView mFocusView;

    @Override
    protected void onResume() {
        super.onResume();
        mCameraView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraView.onPause();
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getPermission();

        mCameraView = binding.surfaceView;
        mFocusView = binding.cameraFocusView;
        // 聚焦监听
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

    private void getPermission() {
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }
}
