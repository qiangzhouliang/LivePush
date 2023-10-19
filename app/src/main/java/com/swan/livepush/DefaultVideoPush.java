package com.swan.livepush;

import android.content.Context;

import javax.microedition.khronos.egl.EGLContext;

/**
 * Created by hcDarren on 2019/7/13.
 * 默认彩色视频的推流
 */
public class DefaultVideoPush extends BaseVideoPush {
    public DefaultVideoPush(Context context, EGLContext eglContext, int textureId) {
        super(context, eglContext);
        setRenderer(new PushRenderer(context, textureId));
    }
}
