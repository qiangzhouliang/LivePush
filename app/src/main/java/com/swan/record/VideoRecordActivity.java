package com.swan.record;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.swan.camera.widget.CameraFocusView;
import com.swan.camera.widget.CameraView;
import com.swan.livepush.databinding.ActivityTestBinding;
import com.swan.record.widget.RecordProgressButton;

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
    private DefaultVideoRecorder mVideoRecorder;
    private RecordProgressButton mRecordButton;

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
        mRecordButton = binding.recordButton;
        mRecordButton.setMaxProgress(60000);

        mRecordButton.setOnRecordListener(new RecordProgressButton.RecordListener() {
            @Override
            public void onStart() {
                mVideoRecorder = new DefaultVideoRecorder(VideoRecordActivity.this, mCameraView.getEglContext(),
                    mCameraView.getTextureId());
                mVideoRecorder.initVideo(getApplication().getFilesDir().getPath() + "/test.mp3",
                    getApplication().getFilesDir().getPath() + "/live_pusher.mp4",
                    720, 1280);
                mVideoRecorder.startRecord();
                mVideoRecorder.setOnRecordInfoListener(times ->{
                    mRecordButton.setCurrentProgress((int)times);
                    Log.e("TAG", "录制了： "+times);
                });
            }

            @Override
            public void onEnd() {
                mVideoRecorder.stopRecord();
            }
        });
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraView.onDestroy();
        if (mVideoRecorder != null) {
            mVideoRecorder.stopRecord();
        }
    }

    private void getPermission() {
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }
}
