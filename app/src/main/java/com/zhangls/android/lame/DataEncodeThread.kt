package com.zhangls.android.lame

import android.media.AudioRecord
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import java.util.*


abstract class DataEncodeThread : HandlerThread("DataEncodeThread"), AudioRecord.OnRecordPositionUpdateListener {
  lateinit var handler: StopHandler
  private val mTasks: Queue<Task> by lazy { LinkedList<Task>() }


  inner class StopHandler(looper: Looper, private val encodeThread: DataEncodeThread) : Handler(looper) {

    override fun handleMessage(msg: Message) {
      if (msg.what == PROCESS_STOP) {
        // 处理缓冲区中的数据
        while (mTasks.isNotEmpty()) {
          encodeThread.processData(mTasks)
        }
        removeCallbacksAndMessages(null)
        encodeThread.flushAndRelease()
      }
    }
  }

  override fun start() {
    super.start()
    handler = StopHandler(looper, this)
  }

  fun sendStopMessage() {
    handler.sendEmptyMessage(PROCESS_STOP)
  }

  override fun onMarkerReached(recorder: AudioRecord) {
  }

  override fun onPeriodicNotification(recorder: AudioRecord) {
    processData(mTasks)
  }

  /**
   * 从缓冲区中读取并处理数据，使用 lame 编码 MP3
   * @return  从缓冲区中读取的数据的长度，缓冲区中没有数据时返回0
   */
  abstract fun processData(queue: Queue<Task>)

  /**
   * Flush all data left in lame buffer to file
   */
  abstract fun flushAndRelease()

  fun addTask(rawData: Task) {
    mTasks.offer(rawData)
  }


  companion object {
    private const val PROCESS_STOP = 1
  }

  class Task(rawData: ShortArray, val readSize: Int) {
    val data: ShortArray = rawData.clone()
  }
}
