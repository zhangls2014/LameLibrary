package com.zhangls.android.sample

import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.media.AudioManager
import android.os.Bundle
import android.widget.Toast
import com.github.piasy.rxandroidaudio.PlayConfig
import com.github.piasy.rxandroidaudio.RxAudioPlayer
import com.tbruyelle.rxpermissions2.RxPermissions
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import com.zhangls.android.lame.AudioDataCallback
import com.zhangls.android.lame.EncodeThread
import com.zhangls.android.lame.StreamAudioRecorder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


/**
 * 录音测试
 *
 * @author zhangls
 */
class MainActivity : RxAppCompatActivity() {

  private val audioPlayer by lazy { RxAudioPlayer.getInstance() }
  private val permissions by lazy { RxPermissions(this) }
  private var outputFile: File? = null
  private var isRecording = false
  private var encodeThread: EncodeThread? = null
  private var subscribe: Disposable? = null


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    btnRecord.apply {
      setText(R.string.record_start)
      setOnClickListener {
        if (isRecording) {
          stopRecord()
          setText(R.string.record_start)
          isRecording = false
        } else {
          val isGranted = permissions.isGranted(WRITE_EXTERNAL_STORAGE) && permissions.isGranted(RECORD_AUDIO)

          if (isGranted.not()) {
            getPermission()
          } else {
            startRecord()
            setText(R.string.record_stop)
            isRecording = true
          }
        }
      }
    }
    btnPlay.apply {
      setText(R.string.audio_play)
      setOnClickListener { playAudio() }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    encodeThread?.quit()
    subscribe?.let {
      if (it.isDisposed) it.dispose()
    }
  }

  private fun getPermission() {
    subscribe = permissions
      .request(WRITE_EXTERNAL_STORAGE, RECORD_AUDIO)
      .subscribe { granted ->
        if (granted) {
          showToast(getString(R.string.permission_granted))
        } else {
          showToast(getString(R.string.permission_refused))
        }
      }
  }

  private fun startRecord() {
    initRecordConfig()

    encodeThread?.apply {
      val callback = object : AudioDataCallback {
        override fun onAudioData(data: ShortArray, size: Int) {
          addPcmBuffers(data, size)
        }

        override fun onError() {
          recordError()
        }
      }
      StreamAudioRecorder.start(callback, this, handler)
    }
  }

  private fun stopRecord() {
    StreamAudioRecorder.stop()
    encodeThread?.finishEncode()
  }

  private fun initRecordConfig() {
    StreamAudioRecorder.init()
    encodeThread = EncodeThread(quality = 5)
    encodeThread?.start()
    outputFile = File(externalCacheDir?.path + File.separator + System.nanoTime() + ".mp3").let {
      if (it.createNewFile()) {
        encodeThread?.setOutputFile(it)
        it
      } else {
        null
      }
    }
  }

  private fun recordError() {
    btnRecord.post {
      showToast(getString(R.string.record_fail))
      btnRecord.setText(R.string.record_start)
      isRecording = false
    }
  }

  private fun playAudio() {
    if (outputFile == null) {
      showToast(getString(R.string.audio_play_fail))
      return
    }

    audioPlayer.play(
        PlayConfig.file(outputFile)
          .streamType(AudioManager.STREAM_VOICE_CALL)
          .build()
      )
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe()
  }

  private fun showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
  }
}