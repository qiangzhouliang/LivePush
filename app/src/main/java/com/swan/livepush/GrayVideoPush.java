package com.swan.livepush;

import android.content.Context;

import javax.microedition.khronos.egl.EGLContext;

/**
 * 灰色视频推流
 */
public class GrayVideoPush extends BaseVideoPush {
    private PushRenderer mRecorderRenderer;

    public GrayVideoPush(Context context, EGLContext eglContext, int textureId) {
        super(context, eglContext);
        mRecorderRenderer = new PushRenderer(context, textureId);
        setRenderer(mRecorderRenderer);
        mRecorderRenderer.setFragmentRender(R.raw.filter_fragment_gray);
    }
}
