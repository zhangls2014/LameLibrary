package com.zhangls.android.lame

import android.os.Handler
import android.os.Looper
import android.os.Message
import java.lang.ref.WeakReference

/**
 * 消息常量：停止处理
 */
const val MSG_PROCESS_STOP = 1

/**
 * @author zhangls
 */
class StopHandler(looper: Looper, encodeThread: EncodeThread) : Handler(looper) {
  private val thread = WeakReference(encodeThread)

  override fun handleMessage(msg: Message) {
    val encodeThread = thread.get()
    if (msg.what == MSG_PROCESS_STOP && encodeThread != null) {
      // 处理缓冲区中的数据
      while (encodeThread.pcmBuffers.isNotEmpty()) {
        encodeThread.processData(encodeThread.pcmBuffers)
      }

      encodeThread.flushAndRelease()
      removeCallbacksAndMessages(null)
    }
  }
}