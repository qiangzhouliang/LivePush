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
    pLivePush->startTime = RTMP_GetTime();
    // 推流
    while (pLivePush->isPushing){
        // 不断往流媒体服务器上去推
        RTMPPacket *pPacket = pLivePush->pPacketQueue->pop();
        int res = RTMP_SendPacket(pLivePush->pRtmp, pPacket, 1);
        LOGE("res = %d", res);
        RTMPPacket_Free(pPacket);
        free(pPacket);
    }

    return 0;

}
void DZLivePush::initConnect() {
    // 怎么连接
    pthread_t initConnectTid;
    pthread_create(&initConnectTid, NULL, initConnectFun, this);
    pthread_detach(initConnectTid);
}
/**
 * 发送 sps 和 pps 到流媒体服务器
 * @param spsData sps 的数据
 * @param spsLen sps 的数据长度
 * @param ppsData pps 的数据
 * @param ppsLen pps 的数据长度
 */
void DZLivePush::pushSpsPps(jbyte *spsData, jint spsLen, jbyte *ppsData, jint ppsLen) {
    // flv 封装格式
    // frame type : 1关键帧，2 非关键帧 (4bit)
    // CodecID : 7表示 AVC (4bit)  , 与 frame type 组合起来刚好是 1 个字节  0x17
    // fixed : 0x00 0x00 0x00 0x00 (4byte)
    // configurationVersion  (1byte)  0x01版本
    // AVCProfileIndication  (1byte)  sps[1] profile
    // profile_compatibility (1byte)  sps[2] compatibility
    // AVCLevelIndication    (1byte)  sps[3] Profile level
    // lengthSizeMinusOne    (1byte)  0xff   包长数据所使用的字节数

    // sps + pps 的数据
    // sps number            (1byte)  0xe1   sps 个数
    // sps data length       (2byte)  sps 长度
    // sps data                       sps 的内容
    // pps number            (1byte)  0x01   pps 个数
    // pps data length       (2byte)  pps 长度
    // pps data                       pps 的内容

    // body 长度 = spsLen + ppsLen + 上面所罗列出来的 16 字节
    int bodySize = spsLen + ppsLen + 16;
    // 初始化创建 RTMPPacket
    RTMPPacket *pPacket = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(pPacket, bodySize);
    RTMPPacket_Reset(pPacket);

    // 按照上面的协议，开始一个一个给 body 赋值
    char *body = pPacket->m_body;
    int index = 0;

    // CodecID 与 frame type 组合起来刚好是 1 个字节  0x17
    body[index++] = 0x17;
    // fixed : 0x00 0x00 0x00 0x00 (4byte)
    body[index++] = 0x00;
    body[index++] = 0x00;
    body[index++] = 0x00;
    body[index++] = 0x00;
    //0x01版本
    body[index++] = 0x01;
    // sps[1] profile
    body[index++] = spsData[1];
    // sps[2] compatibility
    body[index++] = spsData[2];
    // sps[3] Profile level
    body[index++] = spsData[3];
    // 0xff   包长数据所使用的字节数
    body[index++] = 0xff;

    // 0xe1   sps 个数
    body[index++] = 0xe1;
    // sps 长度 高八位和低八位
    body[index++] = (spsLen >> 8) & 0xff;
    body[index++] = spsLen & 0xff;
    // sps 的内容
    memcpy(&body[index], spsData, spsLen);
    index += spsLen;
    // 0x01   pps 个数
    body[index++] = 0x01;
    // pps 长度 高八位和低八位
    body[index++] = (ppsLen >> 8) & 0xff;
    body[index++] = ppsLen & 0xff;
    // pps 的内容
    memcpy(&body[index], ppsData, ppsLen);

    // 设置 RTMPPacket 的参数
    pPacket->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    pPacket->m_nBodySize = bodySize;
    pPacket->m_nTimeStamp = 0;
    pPacket->m_hasAbsTimestamp = 0;
    pPacket->m_nChannel = 0x04;
    pPacket->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    pPacket->m_nInfoField2 = this->pRtmp->m_stream_id;
    // 添加到发送队列
    pPacketQueue->push(pPacket);
}
/**
 * 发送每一帧的视频数据到服务器
 * @param videoData
 * @param dataLen
 * @param keyFrame
 */
void DZLivePush::pushVideo(jbyte *videoData, jint dataLen, jboolean keyFrame) {
    // frame type : 1关键帧，2 非关键帧 (4bit)
    // CodecID : 7表示 AVC (4bit)  , 与 frame type 组合起来刚好是 1 个字节  0x17
    // fixed : 0x01 0x00 0x00 0x00 (4byte)  0x01  表示 NALU 单元

    // video data length       (4byte)  video 长度
    // video data

    // body 长度 = dataLen + 上面所罗列出来的 9 字节
    int bodySize = dataLen + 9;
    // 初始化创建 RTMPPacket
    RTMPPacket *pPacket = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(pPacket, bodySize);
    RTMPPacket_Reset(pPacket);

    // 按照上面的协议，开始一个一个给 body 赋值
    char *body = pPacket->m_body;
    int index = 0;

    // CodecID 与 frame type 组合起来刚好是 1 个字节  0x17
    if (keyFrame) {
        body[index++] = 0x17;
    } else {
        body[index++] = 0x27;
    }
    // fixed : 0x01 0x00 0x00 0x00 (4byte)  0x01  表示 NALU 单元
    body[index++] = 0x01;
    body[index++] = 0x00;
    body[index++] = 0x00;
    body[index++] = 0x00;

    // (4byte)  video 长度
    body[index++] = (dataLen >> 24) & 0xff;
    body[index++] = (dataLen >> 16) & 0xff;
    body[index++] = (dataLen >> 8) & 0xff;
    body[index++] = dataLen & 0xff;
    // video data
    memcpy(&body[index], videoData, dataLen);

    // 设置 RTMPPacket 的参数
    pPacket->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    pPacket->m_nBodySize = bodySize;
    pPacket->m_nTimeStamp = RTMP_GetTime() - startTime;
    pPacket->m_hasAbsTimestamp = 0;
    pPacket->m_nChannel = 0x04;
    pPacket->m_headerType = RTMP_PACKET_SIZE_LARGE;
    pPacket->m_nInfoField2 = this->pRtmp->m_stream_id;
    pPacketQueue->push(pPacket);
}
