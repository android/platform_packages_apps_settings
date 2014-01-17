LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

timezonepicker_dir := ../../../frameworks/opt/timezonepicker/res
res_dirs := $(timezonepicker_dir) res
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))

LOCAL_JAVA_LIBRARIES := \
    bouncycastle \
    conscrypt \
    telephony-common \

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v4 \
    android-support-v13 \
    jsr305 \
    android-opt-timezonepicker \

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
        $(call all-java-files-under, src) \
        src/com/android/settings/EventLogTags.logtags

LOCAL_PACKAGE_NAME := Settings
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

# TODO: why do i have to do this? Calendar doesn't do this!
LOCAL_AAPT_FLAGS += -A $(LOCAL_PATH)/../../../frameworks/opt/timezonepicker/assets

LOCAL_AAPT_FLAGS += -c zz_ZZ
LOCAL_AAPT_FLAGS += --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages com.android.timezonepicker

LOCAL_ADDITIONAL_DEPENDENCIES += $(LOCAL_PATH)/Android.mk

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
