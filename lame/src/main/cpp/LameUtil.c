#include "lamemp3/lame.h"
#include "LameUtil.h"
#include <stdio.h>
#include <jni.h>

static lame_global_flags *lgf = NULL;


JNIEXPORT void JNICALL Java_com_zhangls_android_lame_LameManager_init(
        JNIEnv *env, jclass cls, jint inSampleRate, jint inChannel,
        jint outSampleRate, jint outBitrate, jint quality) {
    if (lgf != NULL) {
        lame_close(lgf);
        lgf = NULL;
    }
    lgf = lame_init();
    lame_set_in_samplerate(lgf, inSampleRate);
    lame_set_num_channels(lgf, inChannel);//输入流的声道
    lame_set_out_samplerate(lgf, outSampleRate);
    lame_set_brate(lgf, outBitrate);
    lame_set_quality(lgf, quality);
    lame_init_params(lgf);
}

JNIEXPORT jint JNICALL Java_com_zhangls_android_lame_LameManager_encode(
        JNIEnv *env, jclass cls, jshortArray buffer_l,
        jshortArray buffer_r, jint samples, jbyteArray mp3buf) {
    jshort *j_buffer_l = (*env)->GetShortArrayElements(env, buffer_l, NULL);
    jshort *j_buffer_r = (*env)->GetShortArrayElements(env, buffer_r, NULL);

    const jsize mp3buf_size = (*env)->GetArrayLength(env, mp3buf);
    jbyte *j_mp3buf = (*env)->GetByteArrayElements(env, mp3buf, NULL);

    int result = lame_encode_buffer(lgf, j_buffer_l, j_buffer_r, samples, j_mp3buf, mp3buf_size);

    (*env)->ReleaseShortArrayElements(env, buffer_l, j_buffer_l, 0);
    (*env)->ReleaseShortArrayElements(env, buffer_r, j_buffer_r, 0);
    (*env)->ReleaseByteArrayElements(env, mp3buf, j_mp3buf, 0);

    return result;
}

JNIEXPORT jint JNICALL Java_com_zhangls_android_lame_LameManager_flush(
        JNIEnv *env, jclass cls, jbyteArray mp3buf) {
    const jsize mp3buf_size = (*env)->GetArrayLength(env, mp3buf);
    jbyte *j_mp3buf = (*env)->GetByteArrayElements(env, mp3buf, NULL);

    int result = lame_encode_flush(lgf, j_mp3buf, mp3buf_size);

    (*env)->ReleaseByteArrayElements(env, mp3buf, j_mp3buf, 0);

    return result;
}

JNIEXPORT void JNICALL Java_com_zhangls_android_lame_LameManager_close(JNIEnv *env, jclass cls) {
    lame_close(lgf);
    lgf = NULL;
}
