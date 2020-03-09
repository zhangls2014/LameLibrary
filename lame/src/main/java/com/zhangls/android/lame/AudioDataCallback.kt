package com.zhangls.android.lame

import androidx.annotation.WorkerThread

/**
 * Although Android frameworks jni implementation are the same for ENCODING_PCM_16BIT and
 * ENCODING_PCM_8BIT, the Java doc declared that the buffer type should be the corresponding
 * type, so we use different ways.
 *
 * @author zhangls
 */
interface AudioDataCallback {
  @WorkerThread
  fun onAudioData(data: ShortArray, size: Int)

  fun onError()
}