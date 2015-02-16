# Copyright (C) 2011 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/java
LOCAL_SRC_FILES := \
	java/com/android/internal/tedongle/ISms.aidl \
    java/com/android/internal/tedongle/IIccPhoneBook.aidl \
    java/com/android/internal/tedongle/EventLogTags.logtags \
	java/com/android/internal/tedongle/ITedongleStateListener.aidl \
	java/com/android/internal/tedongle/IPhoneSubInfo.aidl \
	java/com/android/internal/tedongle/ITelephony.aidl \
	java/com/android/internal/tedongle/ITelephonyRegistry.aidl \
	java/com/android/internal/tedongle/IWapPushManager.aidl \
	java/com/android/internal/tedongle/ITedongle.aidl \
	java/com/android/internal/tedongle/IPhoneStateListener.aidl \	


LOCAL_SRC_FILES += $(call all-java-files-under, java)

# Include AIDL files from mediatek-common.
#LOCAL_AIDL_INCLUDES += $(MTK_PATH_SOURCE)frameworks/common/src

#LOCAL_JAVA_LIBRARIES := mediatek-common
#LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_JAVA_LIBRARIES := voip-common
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := tedongle-telephony

include $(BUILD_JAVA_LIBRARY)

# Include subdirectory makefiles
# ============================================================
include $(call all-makefiles-under,$(LOCAL_PATH))


