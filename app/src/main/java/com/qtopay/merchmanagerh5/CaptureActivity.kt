package com.qtopay.litemall

import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Vibrator
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import com.qtopay.merchmanagerh5.R
import com.qtopay.merchmanagerh5.scaner.CaptureActivityHandler
import com.qtopay.merchmanagerh5.statusbar.StatusBarCompat
import com.zbar.lib.CameraManager
import com.zbar.lib.InactivityTimer
import kotlinx.android.synthetic.main.activity_capture.*
import java.io.IOException

class CaptureActivity : AppCompatActivity(), SurfaceHolder.Callback {
    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        hasSuface = false
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        if (!hasSuface) {
            hasSuface = true
            initCamera(holder!!)
        }
    }

    public var x = 0
    public var y = 0
    public var cropWidth = 0
    public var cropHeight = 0
    public var handler: CaptureActivityHandler? = null
    private var inactivityTimer: InactivityTimer? = null
    var hasSuface = false
    var playBeep = false
    var mediaPlayer: MediaPlayer? = null
    var vibrate: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)
        StatusBarCompat.setStatusBarColor(
            this, ContextCompat.getColor(
                this,
                R.color.white
            ), true
        )
        //请求权限
        CameraManager.init(application)
        inactivityTimer = InactivityTimer(this)
    }

    @Suppress("DEPRECATION")
    override fun onResume() {
        super.onResume()
        var holder = capture_preview.holder
        if (hasSuface) {
            initCamera(holder)
        } else {
            holder.addCallback(this)
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        }
        playBeep = true
        val audioService = getSystemService(AUDIO_SERVICE) as AudioManager
        if (audioService.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            playBeep = false
        }
        initBeepSound()
        vibrate = true
    }

    override fun onPause() {
        super.onPause()
        if (handler != null) {
            handler!!.quitSynchronously()
            handler = null
        }
        CameraManager.get().closeDriver()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun initCamera(surfaceHolder: SurfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder)
            val point = CameraManager.get().cameraResolution
            val width = point.y
            val height = point.x
            x = capture_crop_layout.left * width / container.width
            y = capture_crop_layout.top * height / container.height
            cropWidth = capture_crop_layout.width * width / container.width
            cropHeight = capture_crop_layout.height * height / container.height
        } catch (ioe: IOException) {
            return
        } catch (e: RuntimeException) {
            return
        }

        if (handler == null) {
            handler = CaptureActivityHandler(this@CaptureActivity)
        }
    }

    private fun initBeepSound() {
        if (playBeep && mediaPlayer == null) {
            volumeControlStream = AudioManager.STREAM_MUSIC
            mediaPlayer = MediaPlayer()
            mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mediaPlayer!!.setOnCompletionListener(MediaPlayer.OnCompletionListener { mediaPlayer ->
                mediaPlayer.seekTo(
                    0
                )
            })

            val file = resources.openRawResourceFd(R.raw.beep)
            try {
                mediaPlayer!!.setDataSource(
                    file.fileDescriptor,
                    file.startOffset, file.length
                )
                file.close()
                mediaPlayer!!.setVolume(0.50F, 0.50F)
                mediaPlayer!!.prepare()
            } catch (e: IOException) {
                mediaPlayer = null
            }

        }
    }

    private fun playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer!!.start()
        }
        if (vibrate) {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(200L)
        }
    }

    public fun handleDecode(result: String) {
        playBeepSoundAndVibrate()
        var intent = Intent()
        intent.putExtra("qrcode", result)
        setResult(2, intent)
        finish()
    }
}
