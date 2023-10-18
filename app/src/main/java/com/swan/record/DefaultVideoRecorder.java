package com.swan.record;

import android.content.Context;

import javax.microedition.khronos.egl.EGLContext;

public class DefaultVideoRecorder extends BaseVideoRecorder {
    public DefaultVideoRecorder(Context context, EGLContext eglContext, int textureId) {
        super(context, eglContext);
        setRenderer(new RecorderRenderer(context, textureId));
    }
}
