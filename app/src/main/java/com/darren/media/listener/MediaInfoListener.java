package com.darren.media.listener;

/**
 * Created by hcDarren on 2019/7/14.
 */
public interface MediaInfoListener {
    void musicInfo(int sampleRate, int channels);

    void callbackPcm(byte[] pcmData, int size);
}
