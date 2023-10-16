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