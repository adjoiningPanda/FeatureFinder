LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

OPENCV_LIB_TYPE:=STATIC
OPENCV_INSTALL_MODULES:=on

include C:\Work\OpenCV4Android\OpenCV-2.4.5-android-sdk\sdk\native\jni\OpenCV.mk

LOCAL_MODULE    := native_sample
LOCAL_SRC_FILES := jni_part.cpp \
                   afreader.cpp \
                   asmmodel.cpp \
                   modelfile.cpp \
                   modelimage.cpp \
                   shapeinfo.cpp \
                   shapemodel.cpp \
                   shapevec.cpp \
                   similaritytrans.cpp \

LOCAL_LDLIBS +=  -llog -ldl

include $(BUILD_SHARED_LIBRARY)
