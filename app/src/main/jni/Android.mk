LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

OPENCV_CAMERA_MODULES:=off
OPENCV_INSTALL_MODULES:=off
OPENCV_LIB_TYPE:=STATIC

include /home/pateheo/DEV/OpenCV-2.4.9-android-sdk/sdk/native/jni/OpenCV.mk
#include /home/pateheo/DEV/NVPACK/OpenCV-2.4.8.2-Tegra-sdk/sdk/native/jni/OpenCV-tegra3.mk

LOCAL_LDLIBS += -llog -ldl #-ltbb -L/home/pateheo/DEV/OpenCV-2.4.10-android-sdk/sdk/native/3rdparty/libs/armeabi-v7a
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_SRC_FILES := PointsTracker_jni.cpp
LOCAL_MODULE := lktracker

include $(BUILD_SHARED_LIBRARY)