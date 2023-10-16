package com.swan.livepush;

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

    private String mLiveUrl;
    private ConnectListener mConnectListener;
    public LivePush(String liveUrl){
        this.mLiveUrl = liveUrl;
    }

    public void setOnConnectListener(ConnectListener mConnectListener) {
        this.mConnectListener = mConnectListener;
    }

    public void initConnect(){
        nInitConnect(mLiveUrl);
    }

    private native void nInitConnect(String mLiveUrl);

    // 连接的回调
    // call from jni
    public void onConnectError(int errCode, String errMsg){
        if (this.mConnectListener != null){
            this.mConnectListener.connectError(errCode, errMsg);
        }
    }
    // call from jni
    public void onConnectSuccess(){
        if (this.mConnectListener != null){
            this.mConnectListener.connectSuccess();
        }
    }
}
