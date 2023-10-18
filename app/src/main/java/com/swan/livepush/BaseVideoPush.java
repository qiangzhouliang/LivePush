package com.swan.livepush;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;

import com.swan.opengl.EglHelper;
import com.swan.record.intf.RecordListener;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.concurrent.CyclicBarrier;

import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by hcDarren on 2019/7/13.
 */

public abstract class BaseVideoPush {

    private WeakReference<BaseVideoPush> mVideoRecorderWr = new WeakReference<>(this);
    /**
     * 硬编码 MediaCodec 的 surface
     */
    private Surface mSurface;
    /**
     * 相机共享的 egl 上下文
     */
    private EGLContext mEglContext;
    private Context mContext;

    private GLSurfaceView.Renderer mRenderer;

    private VideoRenderThread mRenderThread;
    private VideoEncoderThread mVideoThread;
    private AudioEncoderThread mAudioThread;

    private MediaCodec mVideoCodec;
    private MediaCodec mAudioCodec;
    private CyclicBarrier mStartCb = new CyclicBarrier(2);
    private CyclicBarrier mDestroyCb = new CyclicBarrier(2);

    public void setRenderer(GLSurfaceView.Renderer renderer) {
        this.mRenderer = renderer;
        mRenderThread = new VideoRenderThread(mVideoRecorderWr);
    }

    public BaseVideoPush(Context context, EGLContext eglContext) {
        this.mContext = context;
        this.mEglContext = eglContext;
    }

    private void initAudioCodec(int sampleRate, int channels) throws IOException {
        MediaFormat audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, sampleRate * channels * 2);
        // 创建音频编码器
        mAudioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        mAudioCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // 开启一个编码采集 音乐播放器回调的 PCM 数据，合成视频
        mAudioThread = new AudioEncoderThread(mVideoRecorderWr);
        // 开启 start AudioCodec
        mAudioCodec.start();
    }

    /**
     * 初始化参数
     *
     * @param videoWidth  录制的宽度
     * @param videoHeight 录制的高度
     */
    public void initVideo(int videoWidth, int videoHeight) {
        try {
            mRenderThread.setSize(videoWidth, videoHeight);
            initVideoCodec(videoWidth, videoHeight);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startPush() {
        mRenderThread.start();
        mVideoThread.start();
        // mAudioThread.start();
    }

    public void stopPush() {
        mRenderThread.requestExit();
        mVideoThread.requestExit();
        // mAudioThread.requestExit();
    }

    /**
     * 初始化视频的 MediaCodec
     *
     * @param width
     * @param height
     */
    private void initVideoCodec(int width, int height) throws IOException {
        MediaFormat videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        // 设置颜色格式
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4);
        // 设置帧率
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 24);
        // 设置 I 帧的间隔时间
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        // 创建编码器
        mVideoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        mVideoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mSurface = mVideoCodec.createInputSurface();
        // 开启一个编码采集 InputSurface 上的数据，合成视频
        mVideoThread = new VideoEncoderThread(mVideoRecorderWr);
    }

    /**
     * 视频的编码线程
     */
    private static final class VideoEncoderThread extends Thread {
        private WeakReference<BaseVideoPush> mVideoRecorderWr;
        private volatile boolean mShouldExit = false;
        private MediaCodec mVideoCodec;
        private MediaCodec.BufferInfo mBufferInfo;
        private long mVideoPts = 0;
        private final CyclicBarrier mStartCb, mDestroyCb;
        private byte[] mVideoSps,mVideoPps;

        public VideoEncoderThread(WeakReference<BaseVideoPush> videoRecorderWr) {
            this.mVideoRecorderWr = videoRecorderWr;
            mVideoCodec = videoRecorderWr.get().mVideoCodec;
            mBufferInfo = new MediaCodec.BufferInfo();
            mStartCb = videoRecorderWr.get().mStartCb;
            mDestroyCb = videoRecorderWr.get().mDestroyCb;
        }

        @Override
        public void run() {
            try {
                mVideoCodec.start();

                while (true) {
                    if (mShouldExit) {
                        return;
                    }

                    BaseVideoPush videoRecorder = mVideoRecorderWr.get();
                    if (videoRecorder == null) {
                        return;
                    }

                    // 代码先不写，先测试，从 surface 上获取数据，编码成 h264 ,通过 MediaMuxer 合成 mp4
                    int outputBufferIndex = mVideoCodec.dequeueOutputBuffer(mBufferInfo, 0);
                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // mStartCb.await();

                        // 获取 sps 和 pps
                        Log.e("TAG", "获取 sps 和 pps");
                        ByteBuffer byteBuffer = mVideoCodec.getOutputFormat().getByteBuffer("csd-0");
                        mVideoSps = new byte[byteBuffer.remaining()];
                        byteBuffer.get(mVideoSps, 0, mVideoSps.length);

                        Log.e("sps", bytesToHexString(mVideoSps));

                        byteBuffer = mVideoCodec.getOutputFormat().getByteBuffer("csd-1");
                        mVideoPps = new byte[byteBuffer.remaining()];
                        byteBuffer.get(mVideoPps, 0, mVideoPps.length);

                        Log.e("pps", bytesToHexString(mVideoPps));
                    } else {
                        while (outputBufferIndex >= 0) {
                            // 获取数据
                            ByteBuffer outBuffer = mVideoCodec.getOutputBuffers()[outputBufferIndex];
                            outBuffer.position(mBufferInfo.offset);
                            outBuffer.limit(mBufferInfo.offset + mBufferInfo.size);

                            // 修改 pts
                            if (mVideoPts == 0) {
                                mVideoPts = mBufferInfo.presentationTimeUs;
                            }
                            mBufferInfo.presentationTimeUs -= mVideoPts;

                            byte[] data = new byte[outBuffer.remaining()];
                            outBuffer.get(data, 0, data.length);
                            Log.e("TAG",bytesToHexString(data));

                            // 回调当前录制的时间
                            if (videoRecorder.mRecordListener != null) {
                                videoRecorder.mRecordListener.onTime(mBufferInfo.presentationTimeUs / 1000);
                            }

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
                // mDestroyCb.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void requestExit() {
            mShouldExit = true;
        }
    }

    /**
     * 视频的编码线程
     */
    private static final class AudioEncoderThread extends Thread {
        private WeakReference<BaseVideoPush> mVideoRecorderWr;
        private volatile boolean mShouldExit = false;
        private MediaCodec mAudioCodec;
        private MediaCodec.BufferInfo mBufferInfo;
        private int mAudioTrackIndex = -1;
        private long mAudioPts = 0;
        private final CyclicBarrier mStartCb, mDestroyCb;

        public AudioEncoderThread(WeakReference<BaseVideoPush> videoRecorderWr) {
            this.mVideoRecorderWr = videoRecorderWr;
            mAudioCodec = videoRecorderWr.get().mAudioCodec;
            mBufferInfo = new MediaCodec.BufferInfo();
            mStartCb = videoRecorderWr.get().mStartCb;
            mDestroyCb = videoRecorderWr.get().mDestroyCb;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    if (mShouldExit) {
                        return;
                    }

                    BaseVideoPush videoRecorder = mVideoRecorderWr.get();
                    if (videoRecorder == null) {
                        return;
                    }

                    // 获取音频数据，那这个音频数据从哪里来？音乐播放器里面来，pcm 数据
                    int outputBufferIndex = mAudioCodec.dequeueOutputBuffer(mBufferInfo, 0);
                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // mStartCb.await();
                    } else {
                        while (outputBufferIndex >= 0) {
                            // 获取数据
                            ByteBuffer outBuffer = mAudioCodec.getOutputBuffers()[outputBufferIndex];
                            outBuffer.position(mBufferInfo.offset);
                            outBuffer.limit(mBufferInfo.offset + mBufferInfo.size);

                            // 修改 pts
                            if (mAudioPts == 0) {
                                mAudioPts = mBufferInfo.presentationTimeUs;
                            }
                            mBufferInfo.presentationTimeUs -= mAudioPts;

                            mAudioCodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = mAudioCodec.dequeueOutputBuffer(mBufferInfo, 0);
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
                mAudioCodec.stop();
                mAudioCodec.release();
                mDestroyCb.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void requestExit() {
            mShouldExit = true;
        }
    }

    private RecordListener mRecordListener;

    public void setOnRecordListener(RecordListener recordListener) {
        this.mRecordListener = recordListener;
    }

    /**
     * 视频的渲染线程
     */
    private static final class VideoRenderThread extends Thread {
        private WeakReference<BaseVideoPush> mVideoRecorderWr;
        private volatile boolean mShouldExit = false;
        private EglHelper mEglHelper;
        private boolean mHashCreateContext = false;
        private boolean mHashSurfaceCreated = false;
        private boolean mHashSurfaceChanged = false;
        private int mWidth;
        private int mHeight;

        public VideoRenderThread(WeakReference<BaseVideoPush> videoRecorderWr) {
            this.mVideoRecorderWr = videoRecorderWr;
            mEglHelper = new EglHelper();
        }

        @Override
        public void run() {

            try {
                while (true) {
                    if (mShouldExit) {
                        return;
                    }

                    BaseVideoPush videoRecorder = mVideoRecorderWr.get();
                    if (videoRecorder == null) {
                        return;
                    }

                    // 1. 创建 EGL 上下文
                    if (!mHashCreateContext) {
                        mEglHelper.initCreateEgl(videoRecorder.mSurface, videoRecorder.mEglContext);
                        mHashCreateContext = true;
                    }

                    // 回调 Render
                    GL10 gl = (GL10) mEglHelper.getEglContext().getGL();
                    if (!mHashSurfaceCreated) {
                        videoRecorder.mRenderer.onSurfaceCreated(gl, mEglHelper.getEGLConfig());
                        mHashSurfaceCreated = true;
                    }

                    if (!mHashSurfaceChanged) {
                        videoRecorder.mRenderer.onSurfaceChanged(gl, mWidth, mHeight);
                        mHashSurfaceChanged = true;
                    }

                    videoRecorder.mRenderer.onDrawFrame(gl);

                    mEglHelper.swapBuffers();

                    // 60 fps
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

        private void requestExit() {
            mShouldExit = true;
        }

        public void setSize(int width, int height) {
            this.mWidth = width;
            this.mHeight = height;
        }
    }

    private static String bytesToHexString(byte[] bArr) {
        StringBuffer sb = new StringBuffer(bArr.length);
        String sTmp;

        for (int i = 0; i < bArr.length; i++) {
            sTmp = Integer.toHexString(bArr[i]);
            if (sTmp.length() < 2) {
                sb.append(0);
            }
            sb.append(sTmp.toUpperCase());
        }
        return sb.toString();
    }
}
