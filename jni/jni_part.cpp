#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <vector>
#include <android/log.h>
//new
#include <stdio.h>
#include <math.h>
#include "opencv2\opencv.hpp"
#include "asmmodel.h"
#include "modelfile.h"
#include <cstdio>
#include <string>
//new
using namespace std;
using namespace cv;

//new
using namespace StatModel;

using std::string;
using cv::imshow;
using std::cerr;
using std::endl;


//new

ASMModel asmModel;
cv::CascadeClassifier faceCascade;
extern "C" {
JNIEXPORT void JNICALL Java_org_opencv_samples_tutorial3_Sample3View_FindFeatures(JNIEnv* env, jobject thiz, jint width, jint height, jbyteArray yuv, jintArray bgra)
{
    jbyte* _yuv  = env->GetByteArrayElements(yuv, 0);
    jint*  _bgra = env->GetIntArrayElements(bgra, 0);
//__android_log_write(ANDROID_LOG_DEBUG,"Tag","tukareta00");
    Mat myuv(height + height/2, width, CV_8UC1, (unsigned char *)_yuv);
    Mat mbgra(height, width, CV_8UC4, (unsigned char *)_bgra);
    Mat mgray(height, width, CV_8UC1, (unsigned char *)_yuv);
//__android_log_write(ANDROID_LOG_DEBUG,"Tag","tukareta01");
    //Please make attention about BGRA byte order
    //ARGB stored in java as int array becomes BGRA at native level
    cvtColor(myuv, mbgra, CV_YUV420sp2BGR, 4);
//__android_log_write(ANDROID_LOG_DEBUG,"Tag","tukareta03");
    //vector<KeyPoint> v;

    //FastFeatureDetector detector(50);
    //detector.detect(mgray, v);
    //for( size_t i = 0; i < v.size(); i++ )
        //circle(mbgra, Point(v[i].pt.x, v[i].pt.y), 10, Scalar(0,0,255,255));

    //Mat img;
    //cv::flip(mbgra, img, 1);
//__android_log_write(ANDROID_LOG_DEBUG,"Tag","tukareta04");
    vector< cv::Rect > faces;
    faceCascade.detectMultiScale( mbgra, faces, 1.2, 2, CV_HAAR_FIND_BIGGEST_OBJECT|CV_HAAR_SCALE_IMAGE, Size(160, 160) );
//__android_log_write(ANDROID_LOG_DEBUG,"Tag","tukareta05");
    vector < ASMFitResult > fitResult = asmModel.fitAll(mbgra, faces, 0);
//__android_log_write(ANDROID_LOG_DEBUG,"Tag","tukareta06");
    //asmModel.showResult(mbgra, fitResult);
//__android_log_write(ANDROID_LOG_DEBUG,"Tag","tukareta07");
////__android_log_write(ANDROID_LOG_DEBUG,"Tag","new draw00");
    for (size_t i=0; i<fitResult.size(); i++){
        vector< Point_<int> > V;
////__android_log_write(ANDROID_LOG_DEBUG,"Tag","new draw00_0");
        fitResult[i].toPointList(V);
////__android_log_write(ANDROID_LOG_DEBUG,"Tag","new draw00_1");
        for(int k=0; k<30; k++){
          circle(mbgra, Point(V[k*2].x, V[k*2].y), 3, Scalar(0,0,255,255));
        }
////__android_log_write(ANDROID_LOG_DEBUG,"Tag","new draw00_2");
    }
  
////__android_log_write(ANDROID_LOG_DEBUG,"Tag","new draw01");
    env->ReleaseIntArrayElements(bgra, _bgra, 0);
    env->ReleaseByteArrayElements(yuv, _yuv, 0);
}

char* jstring2String(JNIEnv* env, jstring jstr) 
{ 
    char* rtn = NULL; 
    jclass clsstring = env->FindClass("java/lang/String"); 
    jstring strencode = env->NewStringUTF("utf-8"); 
    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B"); 
    jbyteArray barr= (jbyteArray)env->CallObjectMethod(jstr, mid, strencode); 
    jsize alen = env->GetArrayLength(barr); 
    jbyte* ba = env->GetByteArrayElements(barr, JNI_FALSE); 
    if (alen > 0) 
    { 
        rtn = (char*)malloc(alen + 1); 
        memcpy(rtn, ba, alen); 
        rtn[alen] = 0; 
    } 
    env->ReleaseByteArrayElements(barr, ba, 0); 
    return rtn; 
}

JNIEXPORT void JNICALL Java_org_opencv_samples_tutorial3_Sample3View_readASMModel(JNIEnv* env, jobject thiz, jstring xml, jstring faceCascadePath)
{
    //const char * c_xml = (char *)env->GetStringUTFChars(xml, JNI_FALSE);
    //__android_log_write(ANDROID_LOG_DEBUG,"Tag",jstring2String(env,xml));
    asmModel.loadFromFile(jstring2String(env,xml));
    //__android_log_write(ANDROID_LOG_DEBUG,"Tag","01yomikomi");
    //delete c_xml;
    //env->ReleaseStringCritical(xml, c_xml);
    faceCascade.load(jstring2String(env,faceCascadePath));
    //__android_log_write(ANDROID_LOG_DEBUG,"Tag","02yomikomi");
}

}
