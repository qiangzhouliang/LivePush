#include <jni.h>
#include <string>

#include "DZLivePush.h"
#include "DZConstDefine.h"

DZLivePush *pLivePush = NULL;
DZJNICall *pJniCall = NULL;

JavaVM *pJavaVm = NULL;
// 重写 so 被加载时会调用的一个方法
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *javaVM, void *reserved){
    LOGE("JNI_OnLoad -->");
    pJavaVm = javaVM;
    JNIEnv *env;
    if (javaVM->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_4) != JNI_OK){
        return -1;
    }
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_swan_livepush_LivePush_nInitConnect(JNIEnv *env, jobject thiz, jstring mLiveUrl_) {
    const char *mLiveUrl = env->GetStringUTFChars(mLiveUrl_, 0);
    pJniCall = new DZJNICall(pJavaVm, env, thiz);
    pLivePush = new DZLivePush(mLiveUrl, pJniCall);

    pLivePush->initConnect();

    env->ReleaseStringUTFChars(mLiveUrl_, mLiveUrl);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_swan_livepush_LivePush_nStop(JNIEnv *env, jobject thiz) {
    if (pLivePush != NULL){
        pLivePush->stop();
        delete(pLivePush);
        pLivePush = NULL;
    }

    if (pJniCall != NULL){
        delete(pJniCall);
        pJniCall = NULL;
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_swan_livepush_LivePush_pushSpsPps(JNIEnv *env, jobject thiz, jbyteArray sps_data,
                                           jint sps_len, jbyteArray pps_data, jint pps_len) {
    jbyte *spsData = env->GetByteArrayElements(sps_data, NULL);
    jbyte *ppsData = env->GetByteArrayElements(pps_data, NULL);
    if (pLivePush != NULL){
        pLivePush->pushSpsPps(spsData, sps_len, ppsData, pps_len);
    }

    env->ReleaseByteArrayElements(sps_data, spsData, 0);
    env->ReleaseByteArrayElements(pps_data, ppsData, 0);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_swan_livepush_LivePush_pushVideo(JNIEnv *env, jobject thiz, jbyteArray video_data,
                                          jint data_len, jboolean key_frame) {
    jbyte *videoData = env->GetByteArrayElements(video_data, NULL);
    if (pLivePush != NULL){
        pLivePush->pushVideo(videoData, data_len, key_frame);
    }

    env->ReleaseByteArrayElements(video_data, videoData, 0);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_swan_livepush_LivePush_pushAudio(JNIEnv *env, jobject thiz, jbyteArray audio_data,
                                          jint data_len) {
    jbyte *audioData = env->GetByteArrayElements(audio_data, NULL);
    if (pLivePush != NULL){
        pLivePush->pushAudio(audioData, data_len);
    }

    env->ReleaseByteArrayElements(audio_data, audioData, 0);
}