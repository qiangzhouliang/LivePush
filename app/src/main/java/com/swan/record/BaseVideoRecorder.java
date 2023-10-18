package com.swan.record;


import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;

import com.swan.opengl.EglHelper;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

/**
 * 视频录制
 */
public abstract class BaseVideoRecorder {
    private WeakReference<BaseVideoRecorder> mVideoRecorderWr = new WeakReference<>(this);
    /**
     * 硬编码 MediaCodec 的 Surface
     */
    private Surface mSurface;
    /**
     * 相机共享的 egl 上下文
     */
    private EGLContext mEglContext;
    /**
     * 渲染器
     */
    private GLSurfaceView.Renderer mRenderer;
    private MediaMuxer mMediaMuxer;
    private VideoRenderThread mRenderThread;
    private VideoEncoderThread mVideoThread;

    private Context mContext;
    private MediaCodec mVideoCodec;

    public void setRenderer(GLSurfaceView.Renderer renderer) {
        this.mRenderer = renderer;
        mRenderThread = new VideoRenderThread(mVideoRecorderWr);
    }

    public BaseVideoRecorder(Context context, EGLContext eglContext){
        this.mEglContext = eglContext;
        this.mContext = context;
    }

    /**
     * 初始化参数
     * @param audioPath 背景音乐的地址
     * @param outPath 输出路径
     * @param videoWidth 录制的宽度
     * @param videoHeight 录制的高度
     */
    public void initVideo(String audioPath, String outPath, int videoWidth, int videoHeight){
        try {
            mRenderThread.setSize(videoWidth, videoHeight);
            // 合成 MP4
            mMediaMuxer = new MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            initVideoCodec(videoWidth, videoHeight);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startRecord() {
        mRenderThread.start();
        mVideoThread.start();
        // mAudioEncoderThread.start();
        Log.e("TAG", "startRecord:");
    }

    public void stopRecord() {
        mRenderThread.requestExit();
        mVideoThread.requestExit();
        // mAudioEncoderThread.requestExit();
        Log.e("TAG", "stopRecord:");
    }

    /**
     * 初始化视频的 MediaCodec
     * @param videoWidth
     * @param videoHeight
     */
    private void initVideoCodec(int videoWidth, int videoHeight) throws IOException {
        MediaFormat videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoWidth, videoHeight);
        // 设置颜色格式
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, videoWidth * videoWidth * 4);
        // 设置帧率
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 24);
        // 设置第 i 帧的间隔时间
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        // 创建编码器
        mVideoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        mVideoCodec.configure(videoFormat, null,null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        //相机的像素数据绘制到该 surface 上面
        mSurface = mVideoCodec.createInputSurface();

        // 开启一个编码采集 InputSurface 上的数据，合成视频
        mVideoThread = new VideoEncoderThread(mVideoRecorderWr);
    }


    /**
     * 视频渲染线程
     */
    public static final class VideoRenderThread extends Thread {
        private WeakReference<BaseVideoRecorder> mVideoRecorderWr;
        private volatile boolean mShouldExit;
        private EglHelper mEglHelper;
        /**
         * 是否有创建 egl，如果有创建，则不在创建
         */
        private boolean mHashCreateContext = false;
        private boolean mHashSurfaceCreated = false;
        private boolean mHashSurfaceChanged = false;

        private int mWidth;
        private int mHeight;

        public VideoRenderThread(WeakReference<BaseVideoRecorder> videoRecorderWr) {
            this.mVideoRecorderWr = videoRecorderWr;
            mEglHelper = new EglHelper();
        }

        @Override
        public void run() {
            mShouldExit = false;
            try {
                while (true) {
                    if (mShouldExit) {
                        onDestroy();
                        return;
                    }
                    BaseVideoRecorder videoRecorder = mVideoRecorderWr.get();
                    if (videoRecorder == null){
                        return;
                    }
                    // 1 创建 EGL 上下文
                    if (!mHashCreateContext){
                        mEglHelper.initCreateEgl(videoRecorder.mSurface, videoRecorder.mEglContext);
                        mHashCreateContext = true;
                    }

                    // 回调 Render
                    GL10 gl = (GL10) mEglHelper.getEglContext().getGL();
                    if (!mHashSurfaceCreated){
                        videoRecorder.mRenderer.onSurfaceCreated(gl, mEglHelper.getEGLConfig());
                        mHashSurfaceCreated = true;
                    }

                    if (!mHashSurfaceChanged){
                        videoRecorder.mRenderer.onSurfaceChanged(gl, mWidth, mHeight);
                        mHashSurfaceChanged = true;
                    }

                    videoRecorder.mRenderer.onDrawFrame(gl);
                    mEglHelper.swapBuffers();

                    // 睡一帧的时间 60fps
                    Thread.sleep(16);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                onDestroy();
            }
        }

        private void onDestroy() {
            mEglHelper.destroy();
        }

        public void requestExit() {
            mShouldExit = true;
        }

        public void setSize(int width, int height) {
            this.mWidth = width;
            this.mHeight = height;
        }
    }

    /**
     * 编码视频线程
     */
    public static final class VideoEncoderThread extends Thread {
        private WeakReference<BaseVideoRecorder> mVideoRecorderWr;
        private volatile boolean mShouldExit;

        private MediaCodec mVideoCodec;
        private MediaCodec.BufferInfo mBufferInfo;
        private MediaMuxer mMediaMuxer;
        /**
         * 视频轨道
         */
        private int mVideoTrackIndex = -1;

        private long mVideoPts = 0;

        public VideoEncoderThread(WeakReference<BaseVideoRecorder> videoRecorderWr) {
            this.mVideoRecorderWr = videoRecorderWr;
            mVideoCodec = videoRecorderWr.get().mVideoCodec;
            mBufferInfo = new MediaCodec.BufferInfo();
            mMediaMuxer = videoRecorderWr.get().mMediaMuxer;
        }

        @Override
        public void run() {
            mShouldExit = false;
            mVideoCodec.start();

            try {
                while (true) {
                    if (mShouldExit) {
                        onDestroy();
                        return;
                    }
                    if (mVideoCodec == null){
                        return;
                    }
                    // 从 Surface上获取数据，编码成 h264，通过 MediaMuxer 合成 MP4
                    // 视频轨道
                    int outputBufferIndex = mVideoCodec.dequeueOutputBuffer(mBufferInfo, 0);

                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        mVideoTrackIndex = mMediaMuxer.addTrack(mVideoCodec.getOutputFormat());
                        mMediaMuxer.start();
                    } else {
                        while (outputBufferIndex >= 0) {
                            // 获取数据
                            ByteBuffer outBuffer = mVideoCodec.getOutputBuffers()[outputBufferIndex];
                            outBuffer.position(mBufferInfo.offset);
                            outBuffer.limit(mBufferInfo.offset + mBufferInfo.size);

                            // 修改视频的 pts
                            if (mVideoPts == 0) {
                                mVideoPts = mBufferInfo.presentationTimeUs;
                            }
                            mBufferInfo.presentationTimeUs -= mVideoPts;

                            // 写入数据
                            mMediaMuxer.writeSampleData(mVideoTrackIndex, outBuffer, mBufferInfo);

                            // 回调当前录制时间
                            if (mVideoRecorderWr.get().mRecordInfoListener != null) {
                                mVideoRecorderWr.get().mRecordInfoListener.onTime(mBufferInfo.presentationTimeUs / 1000);
                            }

                            // 释放 OutputBuffer
                            mVideoCodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = mVideoCodec.dequeueOutputBuffer(mBufferInfo, 0);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                onDestroy();
            }
        }

        private void onDestroy() {
            try {
                mVideoCodec.stop();
                mVideoCodec.release();
                mMediaMuxer.stop();
                mMediaMuxer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void requestExit() {
            mShouldExit = true;
        }
    }



    private RecordInfoListener mRecordInfoListener;

    public void setOnRecordInfoListener(RecordInfoListener mRecordInfoListener) {
        this.mRecordInfoListener = mRecordInfoListener;
    }

    public interface RecordInfoListener {
        void onTime(long times);
    }
}
