//
// Created by swan on 2023/10/11.
//

#include "DZPacketQueue.h"
DZPacketQueue::DZPacketQueue() {
    pPacketQueue = new std::queue<RTMPPacket *>();

    pthread_mutex_init(&packetMutex, NULL);
    pthread_cond_init(&packetCond, NULL);
}

void DZPacketQueue::clear() {
    // 需要清楚队列，还需要清楚每个 AVPacket* 的内存数据
    pthread_mutex_lock(&packetMutex);

    while (!pPacketQueue->empty()){
        RTMPPacket *pPacket = pPacketQueue->front();
        pPacketQueue->pop();

        // 释放队列
        RTMPPacket_Free(pPacket);
        free(pPacket);
    }

    pthread_mutex_unlock(&packetMutex);
}

DZPacketQueue::~DZPacketQueue() {
    if (pPacketQueue != NULL){
        clear();
        delete pPacketQueue;
        pPacketQueue = NULL;
    }

    pthread_mutex_destroy(&packetMutex);
    pthread_cond_destroy(&packetCond);
}

void DZPacketQueue::push(RTMPPacket *pPacket) {
    pthread_mutex_lock(&packetMutex);
    pPacketQueue->push(pPacket);
    // 通知消费者消费
    pthread_cond_signal(&packetCond);
    pthread_mutex_unlock(&packetMutex);
}

RTMPPacket *DZPacketQueue::pop() {
    RTMPPacket *pPacket = NULL;
    pthread_mutex_lock(&packetMutex);
    // 有可能有一直在这儿等，没法退出的情况
    if (pPacketQueue->empty()){
        pthread_cond_wait(&packetCond, &packetMutex);
    } else {
        pPacket = pPacketQueue->front();
        pPacketQueue->pop();
    }
    pthread_mutex_unlock(&packetMutex);

    return pPacket;
}

void DZPacketQueue::notify() {
    pthread_mutex_lock(&packetMutex);
    // 通知消费者消费
    pthread_cond_signal(&packetCond);
    pthread_mutex_unlock(&packetMutex);
}
