package com.swan.livepush;

public interface ConnectListener {
    void connectError(int errCode, String errMsg);

    void connectSuccess();
}
