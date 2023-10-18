package com.swan.livepush;

import android.os.Handler;
import android.os.Looper;

/**
 * @ClassName LivePush
 * @Description
 * @Author swan
 * @Date 2023/10/16 19:53
 **/
public class LivePush {
    static {
        System.loadLibrary("livepush");
    }

    /**
     * 主线程 handle
     */
    private static Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private String mLiveUrl;
    private ConnectListener mConnectListener;
    public void setOnConnectListener(ConnectListener mConnectListener) {
        this.mConnectListener = mConnectListener;
    }

    public LivePush(String liveUrl){
        this.mLiveUrl = liveUrl;
    }

    public void initConnect(){
        nInitConnect(mLiveUrl);
    }

    private native void nInitConnect(String mLiveUrl);

    // 连接的回调
    // call from jni
    public void onConnectError(int errCode, String errMsg){
        stop();
        if (this.mConnectListener != null){
            this.mConnectListener.connectError(errCode, errMsg);
        }
    }

    public void stop() {
        MAIN_HANDLER.post(() -> {
            nStop();
        });
    }

    private native void nStop();

    // call from jni
    public void onConnectSuccess(){
        if (this.mConnectListener != null){
            this.mConnectListener.connectSuccess();
        }
    }

    public native void pushSpsPps(byte[] spsData, int spsLen, byte[] ppsData, int ppsLen);
}
