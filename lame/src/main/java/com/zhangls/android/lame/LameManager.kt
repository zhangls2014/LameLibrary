package com.zhangls.android.lame

import android.media.AudioFormat

/**
 * 默认采样率
 */
const val DEFAULT_SAMPLE_RATE = 44100

/**
 * 默认声道
 */
const val DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO

/**
 * 默认采样位数
 */
const val DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

/**
 * 默认比特率
 */
const val DEFAULT_BIT_RATE = 32

/**
 * 默认音频质量
 */
const val DEFAULT_AUDIO_QUALITY = 7

/**
 * 默认帧数
 */
const val DEFAULT_FRAME_COUNT = 160

/**
 * @author zhangls
 */
class LameManager {

    init {
      System.loadLibrary("lamemp3")
    }

    /**
     * Initialize LAME.
     *
     * @param inSampleRate input sample rate in Hz.
     * @param inChannel number of channels in input stream.
     * @param outSampleRate sample rate in Hz.
     * @param outBitrate brate compression ratio in KHz.
     * @param quality
     *
     * quality=0..9. 0=best (very slow). 9=worst.
     *
     * recommended:
     * 2 near-best quality, not too slow
     * 5 good quality, fast
     * 7 ok quality, really fast
     */
    external fun init(inSampleRate: Int, inChannel: Int, outSampleRate: Int, outBitrate: Int, quality: Int)

    /**
     * Encode buffer to mp3.
     *
     * @param bufferLeft PCM data for left channel.
     * @param bufferRight PCM data for right channel.
     * @param samples number of samples per channel.
     * @param mp3buf result encoded MP3 stream. You must specified "7200 + (1.25 * buffer_l.length)" length array.
     *
     * @return number of bytes output in mp3buf. Can be 0.
     * -1: mp3buf was too small
     * -2: malloc() problem
     * -3: lame_init_params() not called
     * -4: psycho acoustic problems
     */
    external fun encode(bufferLeft: ShortArray, bufferRight: ShortArray, samples: Int, mp3buf: ByteArray): Int

    /**
     * Flush LAME buffer.
     *
     * REQUIRED:
     * lame_encode_flush will flush the intenal PCM buffers, padding with
     * 0's to make sure the final frame is complete, and then flush
     * the internal MP3 buffers, and thus may return a
     * final few mp3 frames.  'mp3buf' should be at least 7200 bytes long
     * to hold all possible emitted data.
     *
     * will also write id3v1 tags (if any) into the bitstream
     *
     * return code = number of bytes output to mp3buf. Can be 0
     * @param mp3buf result encoded MP3 stream. You must specified at least 7200 bytes.
     * @return number of bytes output to mp3buf. Can be 0.
     */
    external fun flush(mp3buf: ByteArray): Int

    /**
     * Close LAME.
     */
    external fun close()
}