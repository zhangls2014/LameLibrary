package com.zhangls.android.lame

import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Bundle
import android.widget.Toast
import com.github.piasy.rxandroidaudio.PlayConfig
import com.github.piasy.rxandroidaudio.RxAudioPlayer
import com.tbruyelle.rxpermissions2.RxPermissions
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


class MainActivity : RxAppCompatActivity() {

  private val mStreamAudioRecorder by lazy { StreamAudioRecorder.instance }
  private val mAudioPlayer by lazy { RxAudioPlayer.getInstance() }
  private var mFileOutputStream: FileOutputStream? = null
  private val mPermissions by lazy { RxPermissions(this) }
  private lateinit var mOutputFile: File
  private var mIsRecording = false
  private val lame by lazy { LameUtil() }
  private lateinit var mEncodeThread: DataEncodeThread


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    btnRecord.text = "Record"
    btnPlay.text = "Play"

    lame.init(
        StreamAudioRecorder.DEFAULT_SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        StreamAudioRecorder.DEFAULT_SAMPLE_RATE,
        32,
        0
    )
    mEncodeThread = object : DataEncodeThread() {
      override fun processData(queue: Queue<Task>) {
        if (queue.isNotEmpty()) {
          val task = queue.poll()
          val size = task.readSize
          val short = task.data
          val mBuffer = ByteArray((7200 + 1.25 * size).toInt())
          val encode = lame.encode(short, short, size, mBuffer)

          mFileOutputStream?.write(mBuffer, 0, encode)
        }
      }

      override fun flushAndRelease() {
        try {
          lame.close()
          mFileOutputStream!!.close()
          mFileOutputStream = null
        } catch (e: IOException) {
          e.printStackTrace()
        }
      }
    }
    mEncodeThread.start()

    btnPlay.setOnClickListener { play() }
    btnRecord.setOnClickListener {
      if (mIsRecording) {
        stopRecord()
        btnRecord!!.text = "Record"
        mIsRecording = false
      } else {
        val isGranted =
            mPermissions.isGranted(WRITE_EXTERNAL_STORAGE) && mPermissions.isGranted(RECORD_AUDIO)

        if (isGranted.not()) {
          mPermissions
              .request(WRITE_EXTERNAL_STORAGE, RECORD_AUDIO)
              .subscribe { granted ->
                if (granted) {
                  Toast.makeText(
                      applicationContext, "Permission granted",
                      Toast.LENGTH_SHORT
                  ).show()
                } else {
                  Toast.makeText(
                      applicationContext,
                      "Permission not granted", Toast.LENGTH_SHORT
                  ).show()
                }
              }
        } else {
          startRecord()
          btnRecord.text = "Stop"
          mIsRecording = true
        }
      }
    }
  }

  private fun startRecord() {
    try {
      mOutputFile = File(externalCacheDir?.path + File.separator + System.nanoTime() + ".mp3")
      mOutputFile.createNewFile()
      mFileOutputStream = FileOutputStream(mOutputFile, true)
      val callback = object : StreamAudioRecorder.AudioDataCallback {
        override fun onAudioData(data: ShortArray, size: Int) {
          mEncodeThread.addTask(DataEncodeThread.Task(data, size))
        }

        override fun onError() {
          recordError()
        }
      }
      mStreamAudioRecorder.start(callback, mEncodeThread, mEncodeThread.handler)
    } catch (e: IOException) {
      e.printStackTrace()
      recordError()
    }
  }

  private fun stopRecord() {
    mStreamAudioRecorder.stop()
    mEncodeThread.sendStopMessage()
  }

  private fun recordError() {
    btnRecord.post {
      Toast.makeText(
          applicationContext, "Record fail",
          Toast.LENGTH_SHORT
      ).show()
      btnRecord.text = "Record"
      mIsRecording = false
    }
  }

  private fun play() {
    mAudioPlayer.play(
        PlayConfig.file(mOutputFile)
            .streamType(AudioManager.STREAM_VOICE_CALL)
            .build())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe()
  }
}
