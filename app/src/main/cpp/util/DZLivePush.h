#ifndef LIVEPUSH_DZLIVEPUSH_H
#define LIVEPUSH_DZLIVEPUSH_H

#include "DZJNICall.h"
#include "DZPacketQueue.h"
#include "DZConstDefine.h"
#include <cstdlib>
#include <string.h>

class DZLivePush {
public:
    DZJNICall *pJniCall = NULL;
    char *liveUrl = NULL;
    DZPacketQueue *pPacketQueue = NULL;
    RTMP *pRtmp = NULL;
    bool isPushing = true;

public:
    DZLivePush(const char *liveUrl, DZJNICall *pJniCall);

    virtual ~DZLivePush();

    /**
     * 初始化连接
     */
    void initConnect();

    void pushSpsPps(jbyte *spsData, jint spsLen, jbyte *ppsData, jint ppsLen);

};


#endif //LIVEPUSH_DZLIVEPUSH_H
