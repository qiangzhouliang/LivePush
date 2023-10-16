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
    public LivePush(String liveUrl){
        this.mLiveUrl = liveUrl;
    }

    public void initConnect(){
        nInitConnect(mLiveUrl);
    }

    private native void nInitConnect(String mLiveUrl);

    // 连接的回调
}
