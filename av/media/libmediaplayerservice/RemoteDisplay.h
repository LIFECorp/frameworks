/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef REMOTE_DISPLAY_H_

#define REMOTE_DISPLAY_H_

#include <media/IMediaPlayerService.h>
#include <media/IRemoteDisplay.h>
#include <media/stagefright/foundation/ABase.h>
#include <utils/Errors.h>
#include <utils/RefBase.h>

#ifdef MTK_WFD_SINK_SUPPORT
///M: @{
#include "sink/WifiDisplaySink.h"
///@}
#endif

namespace android {

struct ALooper;
struct ANetworkSession;
struct IRemoteDisplayClient;
struct WifiDisplaySource;

struct RemoteDisplay : public BnRemoteDisplay {
    RemoteDisplay(
            const sp<IRemoteDisplayClient> &client,
            const char *iface);
///M: @{
#ifndef ANDROID_DEFAULT_CODE
    RemoteDisplay(const sp<IRemoteDisplayClient> &client, const char *iface, const uint32_t wfdFlags);
#if defined(MTK_WFD_SINK_SUPPORT)
    RemoteDisplay(const sp<IRemoteDisplayClient> &client, const char *iface, const sp<IGraphicBufferProducer> &bufferProducer);
#endif
    ///M: add for rtsp generic message
    virtual status_t sendGenericMsg(int cmd);
    virtual status_t setBitrateControl(int level);
    virtual int      getWfdParam(int paramType);
#ifdef MTK_WFD_SINK_SUPPORT     
    virtual status_t suspendDisplay(bool suspend, const sp<IGraphicBufferProducer> &bufferProducer);
#ifdef MTK_WFD_SINK_UIBC_SUPPORT
    virtual status_t sendUibcEvent(const String8& eventDesc);
#endif
#endif
#endif /* ANDROID_DEFAULT_CODE */
///@}

    virtual status_t pause();
    virtual status_t resume();
    virtual status_t dispose();

protected:
    virtual ~RemoteDisplay();

private:
    sp<ALooper> mNetLooper;
    sp<ALooper> mLooper;
    sp<ANetworkSession> mNetSession;
    sp<WifiDisplaySource> mSource;

///M: @{    
#if !defined(ANDROID_DEFAULT_CODE) && defined(MTK_WFD_SINK_SUPPORT)

    static const unsigned defaultPort = 7236;
    sp<WifiDisplaySink> mSink;  
    bool mSinkMode;
    bool mDisposed;
    enum {
        kWhatSinkNotify,
    };
#endif 
///@}

    DISALLOW_EVIL_CONSTRUCTORS(RemoteDisplay);
};

}  // namespace android

#endif  // REMOTE_DISPLAY_H_

