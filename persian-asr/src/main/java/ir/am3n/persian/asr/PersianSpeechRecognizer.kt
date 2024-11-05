package ir.am3n.persian.asr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

class PersianSpeechRecognizer private constructor(
    private val context: Context,
    private val maxResults: Int,
    private val partialResults: Boolean,
    private val listener: RecognitionListener?,
    @AudioQuality private val quality: Int,
    private val debug: Boolean
) {


    companion object {

        private const val TAG = "PersianASR"

        const val QUALITY_LOW = 1
        const val QUALITY_MEDIUM = 2
        const val QUALITY_HIGH = 3
        const val QUALITY_BEST = 4

        const val ERROR_FAILED_UNPACK_MODEL = 1
        const val ERROR_CLIENT = 5
        const val ERROR_INSUFFICIENT_PERMISSIONS = 9
        const val ERROR_FAILED_CREATE_RECOGNIZER = 20
        const val ERROR_FAILED_CREATE_SERVICE = 30

        fun createSpeechRecognizer(
            context: Context,
            maxResults: Int = 3,
            partialResults: Boolean = false,
            listener: RecognitionListener? = null,
            @AudioQuality quality: Int = QUALITY_MEDIUM,
            debug: Boolean = false
        ): PersianSpeechRecognizer {
            return PersianSpeechRecognizer(
                context, maxResults, partialResults,
                listener, quality, debug
            )
        }

    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value = [
            QUALITY_LOW,
            QUALITY_MEDIUM,
            QUALITY_HIGH,
            QUALITY_BEST
        ]
    )
    annotation class AudioQuality

    /**
     * The reason speech recognition failed.
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value = [
            ERROR_FAILED_UNPACK_MODEL,
            ERROR_CLIENT,
            ERROR_INSUFFICIENT_PERMISSIONS,
            ERROR_FAILED_CREATE_RECOGNIZER,
            ERROR_FAILED_CREATE_SERVICE
        ]
    )
    annotation class RecognitionError


    private lateinit var recognizer: Recognizer
    private lateinit var service: SpeechService
    private val gson = Gson()

    @Volatile
    private var isCanceled = false

    private val recognitionListener = object : org.vosk.android.RecognitionListener {

        var results = mutableListOf<Result>()
        var lastPartial = Partial(text = "")
        var isSpeeching = false

        fun clear() {
            results = mutableListOf()
            lastPartial = Partial(text = "")
            isSpeeching = false
        }

        override fun onPartialResult(hypothesis: String) {
            if (isCanceled) return
            Log.d(TAG, "onPartialResult()  $hypothesis")
            val partial = gson.fromJson(hypothesis, Partial::class.java)
            if (partial.text != lastPartial.text) {
                if (partial.text.isNotBlank() && !isSpeeching) {
                    isSpeeching = true
                    listener?.onBeginningOfSpeech()
                } else if (partial.text.isBlank() && isSpeeching) {
                    isSpeeching = false
                    listener?.onEndOfSpeech()
                }
                if (partialResults && partial.text.isNotBlank()) {
                    listener?.onPartialResult(partial)
                }
            }
            lastPartial = partial
        }

        override fun onResult(hypothesis: String) {
            if (isCanceled) return
            Log.d(TAG, "onResult()  $hypothesis")
            if (isSpeeching) {
                isSpeeching = false
                listener?.onEndOfSpeech()
            }
            val result = gson.fromJson(hypothesis, Result::class.java).normalized()
            if (result.data.any { it.text.isNotBlank() }) {
                results.add(result)
                listener?.onResult(result)
            }
        }

        override fun onTimeout() {
            if (isCanceled) return
            Log.d(TAG, "onTimeout()")
            isCanceled = true
            service.stop()
            if (isSpeeching) {
                listener?.onEndOfSpeech()
                isSpeeching = false
            }
            if (lastPartial.text.isNotBlank()) {
                results.add(lastPartial.asResult)
            }
            listener?.onFinalResult(results)
            clear()
        }

        override fun onError(exception: Exception) {
            if (isCanceled) return
            Log.e(TAG, "onPartialResult()", exception)
            listener?.onError(ERROR_CLIENT)
        }

        override fun onFinalResult(hypothesis: String?) {}

    }

    init {
        initialize()
    }

    @Synchronized
    private fun initialize(): Boolean {

        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw Error("SpeechRecognizer shouldn't be used from the application's main thread")
        }

        try {
            LibVosk.setLogLevel(if (debug) LogLevel.DEBUG else LogLevel.INFO)
        } catch (t: Throwable) {
            Log.e(TAG, "initialize()", t)
        }

        val model = try {
            Model(
                StorageService.sync(
                    context,
                    "model-small-fa-0.42",
                    "persian-asr-model"
                )
            )
        } catch (t: Throwable) {
            Log.e(TAG, "initialize()", t)
            listener?.onError(ERROR_FAILED_UNPACK_MODEL)
            return false
        }

        try {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                throw Error("Manifest.permission.RECORD_AUDIO isn't granted")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "initialize()", t)
            listener?.onError(ERROR_INSUFFICIENT_PERMISSIONS)
            return false
        }

        val sampleRate = when (quality) {
            QUALITY_LOW -> 8000f
            QUALITY_MEDIUM -> 16000f
            QUALITY_HIGH -> 22050f
            else -> 44100f
        }

        try {
            recognizer = Recognizer(model, sampleRate)
            recognizer.setMaxAlternatives(maxResults)
            recognizer.setPartialWords(false)
            recognizer.setWords(false)
        } catch (t: Throwable) {
            Log.e(TAG, "initialize()", t)
            listener?.onError(ERROR_FAILED_CREATE_RECOGNIZER)
            return false
        }

        try {
            service = SpeechService(recognizer, sampleRate)
            return true
        } catch (t: Throwable) {
            Log.e(TAG, "initialize()", t)
            listener?.onError(ERROR_FAILED_CREATE_SERVICE)
            return false
        }

    }

    /**
     * Starts recognition. After specified timeout listening stops and the
     * endOfSpeech signals about that.
     * <p>
     * timeout - timeout in milliseconds to listen. Negative -1 means no timeout.
     *
     * @return true if recognition was actually started
     */
    @Synchronized
    fun startListening(timeout: Long = -1) {
        if (!::service.isInitialized)
            if (!initialize())
                return
        recognitionListener.clear()
        isCanceled = false
        service.startListening(recognitionListener, timeout.toInt())
        listener?.onReadyForSpeech()
    }

    @Synchronized
    fun cancel() {
        if (::service.isInitialized) {
            isCanceled = true
            service.stop()
            if (recognitionListener.isSpeeching) {
                listener?.onEndOfSpeech()
                recognitionListener.isSpeeching = false
            }
            recognitionListener.clear()
        }
    }

    @Synchronized
    fun stopListening() {
        if (::service.isInitialized) {
            isCanceled = true
            if (recognitionListener.isSpeeching) {
                listener?.onEndOfSpeech()
                recognitionListener.isSpeeching = false
            }
            val result = gson.fromJson(recognizer.result, Result::class.java)
            listener?.onResult(result)
            service.stop()
            recognitionListener.clear()
        }
    }

    fun shutdown() {
        if (::service.isInitialized) {
            service.stop()
            service.shutdown()
        }
    }

}