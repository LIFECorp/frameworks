LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:=                       \
        GenericSource.cpp               \
        HTTPLiveSource.cpp              \
        NuPlayer.cpp                    \
        NuPlayerDecoder.cpp             \
        NuPlayerDriver.cpp              \
        NuPlayerRenderer.cpp            \
        NuPlayerStreamListener.cpp      \
        RTSPSource.cpp                  \
        StreamingSource.cpp             \
        mp4/MP4Source.cpp               \

LOCAL_C_INCLUDES := \
	$(TOP)/frameworks/av/media/libstagefright/httplive            \
	$(TOP)/frameworks/av/media/libstagefright/include             \
	$(TOP)/frameworks/av/media/libstagefright/mpeg2ts             \
	$(TOP)/frameworks/av/media/libstagefright/rtsp                \
	$(TOP)/frameworks/native/include/media/openmax                \
	$(TOP)/frameworks/native/include/media/editor                 \
	$(TOP)/frameworks/av/libvideoeditor/lvpp 

ifeq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
LOCAL_CFLAGS += -DANDROID_DEFAULT_CODE
else

LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/frameworks-ext/av/include/

ifneq ($(TARGET_BUILD_VARIANT),user)
#For MTB support
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/external/mtb/
LOCAL_CFLAGS += -DMTB_SUPPORT
endif

endif

LOCAL_MODULE:= libstagefright_nuplayer

LOCAL_MODULE_TAGS := eng

include $(BUILD_STATIC_LIBRARY)

