package com.swan.livepush;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.swan.livepush.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements ConnectListener {

    private ActivityMainBinding binding;

    private LivePush mLivePush;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mLivePush = new LivePush("rtmp://120.24.85.248/myapp/mystream");
        mLivePush.initConnect();
        mLivePush.setOnConnectListener(this);

    }

    @Override
    public void connectError(int errCode, String errMsg) {
        Log.e("TAG", "connectError: "+errMsg);
    }

    @Override
    public void connectSuccess() {
        Log.e("TAG", "connectSuccess: 可以推流了");
    }
}