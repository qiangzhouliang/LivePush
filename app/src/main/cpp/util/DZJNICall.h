//
// Created by swan on 2023/10/10.
//

#ifndef FMUSICPLAYER_DZJNICALL_H
#define FMUSICPLAYER_DZJNICALL_H
#include <jni.h>

enum ThreadMode{
    THREAD_CHILD,THREAD_MAIN
};

class DZJNICall {
public:
    JavaVM *javaVM;
    JNIEnv *env;
    jmethodID jConnectErrorMid;
    jmethodID jConnectSuccessMid;
    jobject jLiveObj;
public:
    DZJNICall(JavaVM *javaVm, JNIEnv *env, jobject jLiveObj);
    ~DZJNICall();
public:
    void callConnectError(ThreadMode threadMode,int code, char *msg);

    void callConnectSuccess(ThreadMode mode);
};


#endif //FMUSICPLAYER_DZJNICALL_H
