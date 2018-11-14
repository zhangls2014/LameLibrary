#include <jni.h>

#ifndef _Included_com_zhangls_android_lame_LameUtil
#define _Included_com_zhangls_android_lame_LameUtil
#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT void JNICALL Java_com_zhangls_android_lame_LameUtil_init
  (JNIEnv *, jclass, jint, jint, jint, jint, jint);

JNIEXPORT jint JNICALL Java_com_zhangls_android_lame_LameUtil_encode
  (JNIEnv *, jclass, jshortArray, jshortArray, jint, jbyteArray);

JNIEXPORT jint JNICALL Java_com_zhangls_android_lame_LameUtil_flush
  (JNIEnv *, jclass, jbyteArray);

JNIEXPORT void JNICALL Java_com_zhangls_android_lame_LameUtil_close
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
