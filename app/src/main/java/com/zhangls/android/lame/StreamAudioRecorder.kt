package com.zhangls.android.lame

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import androidx.annotation.NonNull
import androidx.annotation.WorkerThread
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.experimental.and

/**
 * @author zhangls
 */
class StreamAudioRecorder private constructor() {

  private val mIsRecording = AtomicBoolean(false)
  private var mExecutorService: ExecutorService? = null


  companion object {
    const val DEFAULT_SAMPLE_RATE = 44100
    const val DEFAULT_BUFFER_SIZE = 2048
    /**
     * 自定义 每160帧作为一个周期，通知一下需要进行编码
     */
    private const val FRAME_COUNT = 160

    val instance = StreamAudioRecorderHolder.holder
  }

  private object StreamAudioRecorderHolder {
    val holder = StreamAudioRecorder()
  }


  @Synchronized
  fun start(@NonNull audioDataCallback: AudioDataCallback,
            listener: AudioRecord.OnRecordPositionUpdateListener,
            handler: Handler): Boolean {
    stop()

    mExecutorService = Executors.newSingleThreadExecutor()
    if (mIsRecording.compareAndSet(false, true)) {
      mExecutorService?.execute(
        AudioRecordRunnable(
          DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, DEFAULT_BUFFER_SIZE,
          audioDataCallback, listener, handler)
      )
      return true
    }
    return false
  }

  @Synchronized
  fun stop() {
    mIsRecording.compareAndSet(true, false)

    if (mExecutorService != null) {
      mExecutorService?.shutdown()
      mExecutorService = null
    }
  }

  /**
   * Although Android frameworks jni implementation are the same for ENCODING_PCM_16BIT and
   * ENCODING_PCM_8BIT, the Java doc declared that the buffer type should be the corresponding
   * type, so we use different ways.
   */
  interface AudioDataCallback {
    @WorkerThread
    fun onAudioData(data: ShortArray, size: Int)

    fun onError()
  }

  private inner class AudioRecordRunnable
  internal constructor(
      sampleRate: Int, channelConfig: Int, private val mAudioFormat: Int,
      private val mByteBufferSize: Int,
      @param:NonNull private val mAudioDataCallback: AudioDataCallback,
      listener: AudioRecord.OnRecordPositionUpdateListener,
      handler: Handler
  ) : Runnable {

    private val mAudioRecord: AudioRecord

    private val mByteBuffer: ByteArray
    private val mShortBuffer: ShortArray
    private val mShortBufferSize: Int

    init {
      val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, mAudioFormat)
      mShortBufferSize = mByteBufferSize / 2
      mByteBuffer = ByteArray(mByteBufferSize)
      mShortBuffer = ShortArray(mShortBufferSize)
      mAudioRecord = AudioRecord(
          MediaRecorder.AudioSource.MIC, sampleRate, channelConfig,
          mAudioFormat, Math.max(minBufferSize, mByteBufferSize)
      )
      mAudioRecord.positionNotificationPeriod = FRAME_COUNT
      mAudioRecord.setRecordPositionUpdateListener(listener, handler)
    }

    override fun run() {
      if (mAudioRecord.state == AudioRecord.STATE_INITIALIZED) {
        try {
          mAudioRecord.startRecording()
        } catch (e: IllegalStateException) {
          mAudioDataCallback.onError()
          return
        }

        while (mIsRecording.get()) {
          val ret: Int
          if (mAudioFormat == AudioFormat.ENCODING_PCM_16BIT) {
            ret = mAudioRecord.read(mShortBuffer, 0, mShortBufferSize)
            if (ret > 0) {
              mAudioDataCallback.onAudioData(mShortBuffer, ret)
            } else {
              onError(ret)
              break
            }
          } else {
            ret = mAudioRecord.read(mByteBuffer, 0, mByteBufferSize)
            if (ret > 0) {
              mAudioDataCallback.onAudioData(byte2shot(mByteBuffer), ret)
            } else {
              onError(ret)
              break
            }
          }
        }
      }
      mAudioRecord.release()
    }

    private fun byte2shot(bData: ByteArray): ShortArray {
      val short = ShortArray(bData.size)
      bData.forEachIndexed { index, byte ->
        short[index] = byte.toShort()
      }
      return short
    }

    private fun short2byte(sData: ShortArray, size: Int, bData: ByteArray): ByteArray {
      if (size > sData.size || size * 2 > bData.size) {
      }
      for (i in 0 until size) {
        bData[i * 2] = (sData[i] and 0x00FF).toByte()
        bData[i * 2 + 1] = (sData[i].shr(8)).toByte()
      }
      return bData
    }

    private fun onError(errorCode: Int) {
      if (errorCode == AudioRecord.ERROR_INVALID_OPERATION) {
        mAudioDataCallback.onError()
      } else if (errorCode == AudioRecord.ERROR_BAD_VALUE) {
        mAudioDataCallback.onError()
      }
    }
  }
}

private fun Short.shr(i: Int): Short = (this.toInt() shr i).toShort()
