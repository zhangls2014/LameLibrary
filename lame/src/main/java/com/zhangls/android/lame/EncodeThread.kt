package com.zhangls.android.lame

import android.media.AudioRecord
import android.os.HandlerThread
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


/**
 * pcm 转 mp3 数据线程
 *
 * @author zhangls
 */
class EncodeThread(
  sampleRate: Int = DEFAULT_SAMPLE_RATE,
  channelConfig: Int = DEFAULT_CHANNEL_CONFIG,
  bitRate: Int = DEFAULT_BIT_RATE,
  quality: Int = DEFAULT_AUDIO_QUALITY
) : HandlerThread("DataEncodeThread"), AudioRecord.OnRecordPositionUpdateListener {
  lateinit var handler: StopHandler
  val pcmBuffers: LinkedList<PCMBuffer> = LinkedList<PCMBuffer>()
  private val lameManager by lazy {
    LameManager().apply {
      init(sampleRate, channelConfig, sampleRate, bitRate, quality)
    }
  }

  /**
   * 文件输出流
   */
  private var fileOutputStream: FileOutputStream? = null


  override fun start() {
    super.start()
    handler = StopHandler(looper, this)
  }

  override fun onMarkerReached(recorder: AudioRecord) {}

  /**
   * 由 AudioRecord 进行回调，满足帧数，通知数据转换
   */
  override fun onPeriodicNotification(recorder: AudioRecord) {
    processData(pcmBuffers)
  }

  /**
   * 从缓冲区中读取并处理数据，使用 lame 编码 MP3。
   *
   * 每调用一次该方法，会将缓冲队列中第一个缓存片段编码
   *
   * @return  从缓冲区中读取的数据的长度，缓冲区中没有数据时返回 0
   */
  fun processData(queue: Queue<PCMBuffer>) {
    if (queue.isNotEmpty()) {
      val pcmBuffer = queue.poll()
      val size = pcmBuffer!!.readSize
      val short = pcmBuffer.data
      val buffer = ByteArray((7200 + 1.25 * size).toInt())
      val encode = lameManager.encode(short, short, size, buffer)

      fileOutputStream?.write(buffer, 0, encode)
    }
  }

  /**
   * Flush all data left in lame buffer to file
   */
  fun flushAndRelease() {
    try {
      val buffer = ByteArray(7200)
      val flush = lameManager.flush(buffer)
      lameManager.close()
      fileOutputStream?.let {
        it.write(buffer, 0, flush)
        it.close()
      }
      fileOutputStream = null
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }

  /**
   * 指定转换后的数据保存的文在
   */
  fun setOutPutFile(file: File) {
    // 允许持续向文件追加字节，而不是从头开始
    fileOutputStream = FileOutputStream(file, true)
  }

  /**
   * 将 pcm 数据添加到缓存队列中
   */
  fun addPcmBuffers(data: ShortArray, size: Int) {
    pcmBuffers.offer(PCMBuffer(data.clone(), size))
  }

  /**
   * 编码结束，通知 lame 转换结束
   */
  fun finishEncode() {
    handler.sendEmptyMessage(MSG_PROCESS_STOP)
  }


  /**
   * 数据缓存类
   */
  data class PCMBuffer(val data: ShortArray, val readSize: Int)
}
