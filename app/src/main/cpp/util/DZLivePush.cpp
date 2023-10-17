//
// Created by swan on 2023/10/16.
//
#include "DZLivePush.h"


DZLivePush::DZLivePush(const char *liveUrl,DZJNICall *pJniCall) :pJniCall(pJniCall){
    this->liveUrl = static_cast<char *>(malloc(strlen(liveUrl) + 1));
    strcpy(this->liveUrl, liveUrl);
    pPacketQueue = new DZPacketQueue();
}

DZLivePush::~DZLivePush() {
    if (pRtmp != NULL){
        // 断开连接
        RTMP_Close(pRtmp);
        free(pRtmp);
        pRtmp = NULL;
    }

    if (liveUrl != NULL){
        free(liveUrl);
        liveUrl = NULL;
    }
    if (pPacketQueue != NULL){
        delete(pPacketQueue);
        pPacketQueue = NULL;
    }
}

void *initConnectFun(void * context){
    DZLivePush *pLivePush = (DZLivePush *)context;
    // 1. 创建 RTMP
    pLivePush->pRtmp = RTMP_Alloc();
    // 2. 初始化
    RTMP_Init(pLivePush->pRtmp);
    // 3. 设置参数，连接超时时间
    pLivePush->pRtmp->Link.timeout = 10;
    pLivePush->pRtmp->Link.lFlags |= RTMP_LF_LIVE;
    RTMP_SetupURL(pLivePush->pRtmp, pLivePush->liveUrl);
    RTMP_EnableWrite(pLivePush->pRtmp);
    // 开始连接
    if (!RTMP_Connect(pLivePush->pRtmp, NULL)){
        // 如果不成功，回调到Java层
        LOGE("RTMP_Connect error");
        pLivePush->pJniCall->callConnectError(THREAD_CHILD, INIT_RTMP_CONNECT_ERROR_CODE, "RTMP_Connect error");
        return reinterpret_cast<void *>(INIT_RTMP_CONNECT_ERROR_CODE);
    }

    if (!RTMP_ConnectStream(pLivePush->pRtmp, 0)){
        // 如果不成功，回调到Java层
        LOGE("RTMP_ConnectStream error");
        pLivePush->pJniCall->callConnectError(THREAD_CHILD, INIT_RTMP_CONNECT_STREAM_ERROR_CODE, "RTMP_ConnectStream error");
        return reinterpret_cast<void *>(INIT_RTMP_CONNECT_STREAM_ERROR_CODE);
    }

    LOGE("rtmp connect success");
    pLivePush->pJniCall->callConnectSuccess(THREAD_CHILD);
    return 0;

}
void DZLivePush::initConnect() {
    // 怎么连接
    pthread_t initConnectTid;
    pthread_create(&initConnectTid, NULL, initConnectFun, this);
    pthread_detach(initConnectTid);
}
