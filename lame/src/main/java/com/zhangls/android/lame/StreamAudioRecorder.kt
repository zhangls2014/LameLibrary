package com.zhangls.android.lame

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean


/**
 * @author zhangls
 */
object StreamAudioRecorder {
  private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
  private var sampleRate: Int = DEFAULT_SAMPLE_RATE
  private var channelConfig: Int = DEFAULT_CHANNEL_CONFIG
  private var audioFormat: Int = DEFAULT_AUDIO_FORMAT

  /**
   * 状态标记：是否正在录音
   */
  private val isRecording = AtomicBoolean(false)

  /**
   * 初始化。配置默认参数
   */
  fun init() {
    init(DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT)
  }

  /**
   * 初始化，配置参数
   */
  @Suppress("MemberVisibilityCanBePrivate")
  fun init(
    sampleRate: Int = DEFAULT_SAMPLE_RATE,
    channelConfig: Int = DEFAULT_CHANNEL_CONFIG,
    audioFormat: Int = DEFAULT_AUDIO_FORMAT
  ) {
    this.sampleRate = sampleRate
    this.channelConfig = channelConfig
    this.audioFormat = audioFormat

    if (isValidSampleRate(sampleRate).not()) throw Exception("系统不支持该音频采样率")

    // 再次调用初始化方法时，如果正在录音，立即取消
    if (isRecording.get()) stop()
  }

  /**
   * 获取手机支持的音频采样率
   */
  private fun isValidSampleRate(sampleRate: Int): Boolean {
    val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    return bufferSize > 0
  }

  @Synchronized
  fun start(
    audioDataCallback: AudioDataCallback,
    listener: AudioRecord.OnRecordPositionUpdateListener,
    handler: Handler
  ): Boolean {
    stop()

    if (isRecording.compareAndSet(false, true)) {
      val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
      executorService.execute(
        AudioRecordRunnable(
          sampleRate,
          channelConfig,
          audioFormat,
          minBufferSize,
          audioDataCallback,
          listener,
          handler
        )
      )
      return true
    }
    return false
  }

  @Synchronized
  fun stop() {
    isRecording.compareAndSet(true, false)
  }

  class AudioRecordRunnable constructor(
    sampleRate: Int,
    channelConfig: Int,
    private val audioFormat: Int,
    minBufferSize: Int,
    private val audioDataCallback: AudioDataCallback,
    listener: AudioRecord.OnRecordPositionUpdateListener,
    handler: Handler
  ) : Runnable {
    private val byteBuffer: ByteArray
    private val shortBuffer: ShortArray
    private val audioRecord: AudioRecord

    init {
      val bytesPerFrame: Int = if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) 2 else 1
      // 计算缓冲区的大小，使其是设置周期帧数的整数倍，方便循环
      var frameSize = minBufferSize / bytesPerFrame
      if (frameSize % DEFAULT_FRAME_COUNT != 0) {
        frameSize += (DEFAULT_FRAME_COUNT - frameSize % DEFAULT_FRAME_COUNT)
      }
      val bufferSize = frameSize * bytesPerFrame

      byteBuffer = ByteArray(bufferSize)
      shortBuffer = ShortArray(bufferSize)

      audioRecord = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate,
        channelConfig,
        audioFormat,
        bufferSize
      ).apply {
        positionNotificationPeriod = DEFAULT_FRAME_COUNT
        setRecordPositionUpdateListener(listener, handler)
      }
    }

    override fun run() {
      if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
        audioRecord.startRecording()

        while (isRecording.get()) {
          val result: Int
          if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
            result = audioRecord.read(shortBuffer, 0, shortBuffer.size)
            if (result > 0) {
              audioDataCallback.onAudioData(shortBuffer, result)
            } else {
              audioDataCallback.onError()
              break
            }
          } else {
            result = audioRecord.read(byteBuffer, 0, byteBuffer.size)
            if (result > 0) {
              audioDataCallback.onAudioData(byte2shot(byteBuffer), result)
            } else {
              audioDataCallback.onError()
              break
            }
          }
        }

        audioRecord.stop()
      }
      audioRecord.release()
    }

    private fun byte2shot(bData: ByteArray): ShortArray {
      val short = ShortArray(bData.size)
      bData.forEachIndexed { index, byte ->
        short[index] = byte.toShort()
      }
      return short
    }
  }
}