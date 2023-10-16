//
// Created by swan on 2023/10/10.
//
#include <jni.h>
#include "DZJNICall.h"
#include "DZConstDefine.h"

DZJNICall::DZJNICall(JavaVM *javaVm, JNIEnv *env, jobject jLiveObj) {
    this->javaVM = javaVm;
    this->env = env;
    this->jLiveObj = env->NewGlobalRef(jLiveObj);


//    jclass jPlayerClass = env->FindClass("com/swan/livepush/LivePush");
    jclass jPlayerClass = env->GetObjectClass(jLiveObj);
    jConnectErrorMid = env->GetMethodID(jPlayerClass, "onConnectError", "(ILjava/lang/String;)V");
    jConnectSuccessMid = env->GetMethodID(jPlayerClass, "onConnectSuccess", "()V");
    LOGE("------------>");
}


DZJNICall::~DZJNICall() {
    env->DeleteGlobalRef(jLiveObj);
}

void DZJNICall::callConnectError(ThreadMode threadMode,int code, char *msg) {
    // 子线程用不了主线程 jniEnv （native 线程）
    // 子线程是不共享 jniEnv ，他们有自己所独有的
    if (threadMode == THREAD_MAIN) {
        jstring jMsg = env->NewStringUTF(msg);
        env->CallVoidMethod(jLiveObj, jConnectErrorMid, code, jMsg);
        env->DeleteLocalRef(jMsg);
    } else if (threadMode == THREAD_CHILD) {
        // 获取当前线程的 JNIEnv， 通过 JavaVM
        JNIEnv *env;
        if (javaVM->AttachCurrentThread(&env, 0) != JNI_OK) {
            LOGE("get child thread jniEnv error!");
            return;
        }

        jstring jMsg = env->NewStringUTF(msg);
        env->CallVoidMethod(jLiveObj, jConnectErrorMid, code, jMsg);
        env->DeleteLocalRef(jMsg);

        javaVM->DetachCurrentThread();
    }
}
/**
 * 回调到java层，告诉他准备好了
 * @param mode
 */
void DZJNICall::callConnectSuccess(ThreadMode threadMode) {
// 子线程用不了主线程 jniEnv （native 线程）
    // 子线程是不共享 jniEnv ，他们有自己所独有的
    if (threadMode == THREAD_MAIN) {
        env->CallVoidMethod(jLiveObj, jConnectSuccessMid);
    } else if (threadMode == THREAD_CHILD) {
        // 获取当前线程的 JNIEnv， 通过 JavaVM
        JNIEnv *env;
        if (javaVM->AttachCurrentThread(&env, 0) != JNI_OK) {
            LOGE("get child thread jniEnv error!");
            return;
        }

        env->CallVoidMethod(jLiveObj, jConnectSuccessMid);
        javaVM->DetachCurrentThread();
    }
}
