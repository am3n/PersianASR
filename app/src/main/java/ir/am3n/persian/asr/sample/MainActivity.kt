package ir.am3n.persian.asr.sample

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.AppCompatButton
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

    private lateinit var tgbStartContinuously: AppCompatToggleButton
    private lateinit var btnStartWithTimeout: AppCompatButton

    private lateinit var recognizer: PersianSpeechRecognizer
    private var ignoreCheckChange = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_main)

        tgbStartContinuously = findViewById(R.id.btnStartContinuously)
        btnStartWithTimeout = findViewById(R.id.btnStartWithTimeout)

        initializeASR()

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
            thread {
                recognizer.startListening(timeout = 10_000)
            }
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000 && grantResults[0] == PERMISSION_GRANTED) {
            initializeASR()
        }
    }

    private fun initializeASR() {

        val recognizerListener = object : RecognitionListener {

            override fun onReadyForSpeech() {
                Log.d("Me-$TAG", "asr > onReadyForSpeech()")
            }

            override fun onBeginningOfSpeech() {
                Log.d("Me-$TAG", "asr > onBeginningOfSpeech()")
            }

            override fun onEndOfSpeech() {
                Log.d("Me-$TAG", "asr > onEndOfSpeech()")
            }

            override fun onPartialResult(partial: Partial) {
                Log.d("Me-$TAG", "asr > onPartialResults() $partial")

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
            }

            override fun onFinalResult(results: List<Result>) {
                Log.d("Me-$TAG", "asr > onFinalResults()  \n\t${results.joinToString("\n\t")}")
            }

            override fun onError(error: Int) {
                Log.e("Me-$TAG", "asr > onError() code: $error", null)
                if (error == PersianSpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity, arrayOf(Manifest.permission.RECORD_AUDIO), 1000
                    )
                }
            }

        }

        thread {
            if (::recognizer.isInitialized) {
                recognizer.shutdown()
            }
            recognizer = PersianSpeechRecognizer.createSpeechRecognizer(
                context = this,
                maxResults = 5,
                partialResults = true,
                listener = recognizerListener,
                quality = PersianSpeechRecognizer.QUALITY_LOW,
                debug = true
            )
        }

    }

}
