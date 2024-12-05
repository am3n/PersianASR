package ir.am3n.persian.asr.sample

import kotlin.concurrent.thread

internal object CpuStressful {

    private var NUM_THREADS: Int = 8
    private var RUNNING_TIME: Int = 120

    @Throws(InterruptedException::class)
    fun start() {
        thread {
            for (i in 0 until NUM_THREADS) {
                RegexThread()
            }
            Thread.sleep(1000L * RUNNING_TIME)
        }
    }

}