package ir.am3n.persian.asr.sample

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.media.AudioManager
import android.media.AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED
import android.media.AudioManager.EXTRA_SCO_AUDIO_STATE
import android.media.AudioManager.SCO_AUDIO_STATE_CONNECTED
import android.media.AudioManager.SCO_AUDIO_STATE_CONNECTING
import android.media.AudioManager.SCO_AUDIO_STATE_DISCONNECTED
import android.media.AudioManager.SCO_AUDIO_STATE_ERROR
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.AppCompatToggleButton
import androidx.core.app.ActivityCompat
import ir.am3n.persian.asr.Partial
import ir.am3n.persian.asr.PersianSpeechRecognizer
import ir.am3n.persian.asr.RecognitionListener
import ir.am3n.persian.asr.Result
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var tgbQuality: AppCompatToggleButton
    private lateinit var tgbStartContinuously: AppCompatToggleButton
    private lateinit var btnStartWithTimeout: AppCompatButton
    private lateinit var btnClearLogs: AppCompatButton
    private lateinit var scrl: ScrollView
    private lateinit var txt: AppCompatTextView

    private lateinit var recognizer: PersianSpeechRecognizer
    private lateinit var recognizerListener: RecognitionListener
    private var ignoreCheckChange = false

    private val adapter = BluetoothAdapter.getDefaultAdapter()

    private val permissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.BLUETOOTH
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_main)

        tgbQuality = findViewById(R.id.btnQuality)
        tgbStartContinuously = findViewById(R.id.btnStartContinuously)
        btnStartWithTimeout = findViewById(R.id.btnStartWithTimeout)
        btnClearLogs = findViewById(R.id.btnClearLogs)
        scrl = findViewById(R.id.scrl)
        txt = findViewById(R.id.txt)

        recognizerListener = object : RecognitionListener {

            override fun onReadyForSpeech() {
                Log.d("Me-$TAG", "asr > onReadyForSpeech()")
                append("on ready")
            }

            override fun onBeginningOfSpeech() {
                Log.d("Me-$TAG", "asr > onBeginningOfSpeech()")
                append("beginning of speech")
            }

            override fun onEndOfSpeech() {
                Log.d("Me-$TAG", "asr > onEndOfSpeech()")
                append("end of speech")
            }

            override fun onPartialResult(partial: Partial) {
                Log.d("Me-$TAG", "asr > onPartialResult() $partial")
                append("partial: ${partial.text}")

                if (partial.text.contains("لغو")) {
                    recognizer.cancel()
                    runOnUiThread {
                        if (tgbStartContinuously.isChecked) {
                            ignoreCheckChange = true
                            tgbStartContinuously.isChecked = false
                        }
                    }

                } else if (partial.text.contains("پایان")) {
                    recognizer.stopListening()
                    runOnUiThread {
                        if (tgbStartContinuously.isChecked) {
                            ignoreCheckChange = true
                            tgbStartContinuously.isChecked = false
                        }
                    }
                }

            }

            override fun onResult(result: Result) {
                Log.d("Me-$TAG", "asr > onResults()  \n\t${result.data.joinToString("\n\t")}")
                append("\nresult:\n\t${result.data.joinToString("\n\t")}\n")
            }

            override fun onFinalResult(results: List<Result>) {
                Log.d("Me-$TAG", "asr > onFinalResults()  \n\t${results.joinToString("\n\t")}")
                append("\nresults:\n\t${results.joinToString("\n\t")}\n")
                runOnUiThread {
                    tgbStartContinuously.isEnabled = true
                    btnStartWithTimeout.isEnabled = true
                }
            }

            override fun onError(error: Int) {
                Log.e("Me-$TAG", "asr > onError() code: $error", null)
                append("error: $error")
                if (error == PersianSpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    requestPermissions()
                }
            }

        }

        tgbQuality.setOnCheckedChangeListener { _, isChecked ->
            if (tgbStartContinuously.isChecked) {
                tgbStartContinuously.isChecked = false
            }
            initializeASR(
                if (isChecked) PersianSpeechRecognizer.QUALITY_BEST
                else PersianSpeechRecognizer.QUALITY_LOW
            )
        }

        tgbStartContinuously.setOnCheckedChangeListener { _, isChecked ->
            btnStartWithTimeout.isEnabled = !isChecked
            if (ignoreCheckChange) {
                ignoreCheckChange = false
                return@setOnCheckedChangeListener
            }
            thread {
                if (isChecked) {
                    recognizer.startListening()
                } else {
                    recognizer.stopListening()
                }
            }
        }

        btnStartWithTimeout.setOnClickListener {
            btnStartWithTimeout.isEnabled = false
            tgbStartContinuously.isEnabled = false
            thread {
                recognizer.startListening(timeout = 10_000)
            }
        }

        btnClearLogs.setOnClickListener {
            txt.text = ""
        }


        var request = false
        permissions.forEach {
            if (ActivityCompat.checkSelfPermission(this, it) != PERMISSION_GRANTED) {
                request = true
            }
        }
        if (request) {
            requestPermissions()
        } else {
            init()
        }

        //CpuStressful.start()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000 && grantResults[0] == PERMISSION_GRANTED) {
            init()
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this@MainActivity,
            permissions,
            1000
        )
    }

    private fun init() {

        checkHeadset()

        registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1)) {
                        BluetoothHeadset.STATE_CONNECTED -> {
                            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }
                            append("headset connected: $device")
                            checkHeadset()
                        }

                        BluetoothHeadset.STATE_DISCONNECTED -> {
                            append("headset disconnected")
                        }
                    }
                }
            },
            IntentFilter(ACTION_CONNECTION_STATE_CHANGED)
        )

        if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
            registerReceiver(
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        intent ?: return
                        when (intent.getIntExtra(EXTRA_SCO_AUDIO_STATE, -2)) {
                            SCO_AUDIO_STATE_CONNECTING -> {
                                append("waiting for bluetooth sco ...")
                            }

                            SCO_AUDIO_STATE_CONNECTED -> {
                                append("bluetooth sco started")
                            }

                            SCO_AUDIO_STATE_DISCONNECTED -> {
                                if (ActivityCompat.checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    ) != PERMISSION_GRANTED
                                ) {
                                    requestPermissions()
                                    return
                                }
                                val state = adapter.getProfileConnectionState(BluetoothProfile.HEADSET)
                                append("bluetooth sco disconnected,  headset-state: $state")
                                if (state == BluetoothAdapter.STATE_CONNECTED) {
                                    val am = getSystemService(AUDIO_SERVICE) as AudioManager
                                    am.stopBluetoothSco()
                                    checkHeadset()
                                }
                            }

                            SCO_AUDIO_STATE_ERROR -> {
                                append("bluetooth sco error")
                            }
                        }
                    }
                },
                IntentFilter(ACTION_SCO_AUDIO_STATE_UPDATED)
            )
        }

        initializeASR(
            if (tgbQuality.isChecked) PersianSpeechRecognizer.QUALITY_BEST
            else PersianSpeechRecognizer.QUALITY_LOW
        )

    }

    private fun initializeASR(quality: Int) {
        thread {
            if (::recognizer.isInitialized) {
                recognizer.stopListening()
                recognizer.shutdown()
            }
            recognizer = PersianSpeechRecognizer.createSpeechRecognizer(
                context = this,
                maxResults = 5,
                partialResults = true,
                listener = recognizerListener,
                quality = quality,
                debug = true
            )
        }
    }

    private fun append(message: String) {
        runOnUiThread {
            txt.append(message)
            txt.append("\n")
            txt.post {
                scrl.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    private fun checkHeadset() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
            requestPermissions()
            return
        }
        val state = adapter.getProfileConnectionState(BluetoothProfile.HEADSET)
        if (state == BluetoothAdapter.STATE_CONNECTING) {
            Handler(Looper.getMainLooper()).postDelayed({
                checkHeadset()
            }, 500)
        }
        if (state == BluetoothAdapter.STATE_CONNECTED) {
            Handler(Looper.getMainLooper()).postDelayed({
                val am = getSystemService(AUDIO_SERVICE) as AudioManager
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                am.setMicrophoneMute(true)
                am.startBluetoothSco()
            }, 500)
        }
    }

}
