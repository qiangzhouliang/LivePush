//
// Created by swan on 2023/10/16.
//

#ifndef LIVEPUSH_DZLIVEPUSH_H
#define LIVEPUSH_DZLIVEPUSH_H


#include "DZJNICall.h"

class DZLivePush {
public:
    DZJNICall *pJniCall = NULL;
    const char *liveUrl;
public:
    DZLivePush(const char *liveUrl, DZJNICall *pJniCall);

    virtual ~DZLivePush();
};


#endif //LIVEPUSH_DZLIVEPUSH_H
