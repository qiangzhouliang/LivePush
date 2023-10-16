//
// Created by swan on 2023/10/16.
//

#include "DZLivePush.h"

DZLivePush::DZLivePush(const char *liveUrl,DZJNICall *pJniCall) :pJniCall(pJniCall){
    this->liveUrl = liveUrl;
}

DZLivePush::~DZLivePush() {

}
