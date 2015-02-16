LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    android_renderscript_RenderScript.cpp

#GMS rename
ifeq ($(strip $(BUILD_GMS)), yes)
LOCAL_SHARED_LIBRARIES := \
        libRSSupport_old \
        libjnigraphics
else
LOCAL_SHARED_LIBRARIES := \
        libRSSupport \
        libjnigraphics
endif

LOCAL_STATIC_LIBRARIES := \
        libcutils \
        liblog

#GMS Rename
ifeq ($(strip $(BUILD_GMS)), yes)
rs_generated_include_dir := $(call intermediates-dir-for,SHARED_LIBRARIES,libRSSupport_old,,)
else
rs_generated_include_dir := $(call intermediates-dir-for,SHARED_LIBRARIES,libRSSupport,,)
endif

LOCAL_C_INCLUDES += \
	$(JNI_H_INCLUDE) \
	frameworks/rs \
	$(rs_generated_include_dir)

LOCAL_CFLAGS +=

LOCAL_LDLIBS := -lpthread
LOCAL_ADDITIONAL_DEPENDENCIES := $(addprefix $(rs_generated_include_dir)/,rsgApiFuncDecl.h)

#GMS rename
ifeq ($(strip $(BUILD_GMS)), yes)
LOCAL_MODULE:= librsjni_old
else
LOCAL_MODULE:= librsjni
endif

LOCAL_ADDITIONAL_DEPENDENCIES += $(rs_generated_source)
LOCAL_MODULE_TAGS := optional

#GMS rename
ifeq ($(strip $(BUILD_GMS)), yes)
LOCAL_REQUIRED_MODULES := libRSSupport_old
else
LOCAL_REQUIRED_MODULES := libRSSupport
endif

include $(BUILD_SHARED_LIBRARY)
