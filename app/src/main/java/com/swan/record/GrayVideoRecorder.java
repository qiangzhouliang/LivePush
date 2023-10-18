package com.swan.record;

import android.content.Context;

import com.swan.livepush.R;

import javax.microedition.khronos.egl.EGLContext;

public class GrayVideoRecorder extends BaseVideoRecorder {
    private  RecorderRenderer mRecorderRenderer;

    public GrayVideoRecorder(Context context, EGLContext eglContext, int textureId) {
        super(context, eglContext);
        mRecorderRenderer = new RecorderRenderer(context, textureId);
        setRenderer(mRecorderRenderer);
        mRecorderRenderer.setFragmentRender(R.raw.filter_fragment_gray);
    }
}
