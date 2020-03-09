#include <jni.h>

#ifndef _Included_com_zhangls_android_lame_LameUtil
#define _Included_com_zhangls_android_lame_LameUtil
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_zhangls_android_lame_LameManager_init
        (JNIEnv *, jobject, jint, jint, jint, jint, jint);

JNIEXPORT jint JNICALL Java_com_zhangls_android_lame_LameManager_encode
        (JNIEnv *, jobject, jshortArray, jshortArray, jint, jbyteArray);

JNIEXPORT jint JNICALL Java_com_zhangls_android_lame_LameManager_flush
        (JNIEnv *, jobject, jbyteArray);

JNIEXPORT void JNICALL Java_com_zhangls_android_lame_LameManager_close
        (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
