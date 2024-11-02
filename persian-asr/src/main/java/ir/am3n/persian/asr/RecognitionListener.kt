package ir.am3n.persian.asr

/**
 * Used for receiving notifications from the PersianSpeechRecognizer when the
 * recognition related events occur. All the callbacks are executed on the
 * Application main thread.
 */
interface RecognitionListener {

    fun onReadyForSpeech()

    fun onBeginningOfSpeech()

    fun onEndOfSpeech()

    fun onPartialResult(partial: Partial)

    fun onResult(result: Result)

    fun onFinalResult(results: List<Result>)

    fun onError(@PersianSpeechRecognizer.RecognitionError error: Int)

}
