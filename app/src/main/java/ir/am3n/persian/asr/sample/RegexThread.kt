package ir.am3n.persian.asr.sample

import java.util.regex.Pattern
import kotlin.math.pow
import kotlin.math.tan
import kotlin.random.Random

internal class RegexThread : Thread("Regex Thread") {

    init {
        start()
    }

    override fun run() {
        var i = 0
        while (true) {
            /*val p = Pattern.compile(
                "[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*" +
                        "[a-z0-9])?\\.)+(?:[A-Z]{2}|com|org|net|edu|gov|mil|biz|info|mobi|name|aero|asia|jobs|museum)\b"
            )*/
            calc()
            i++
            /*if (i % 5 == 0) {
                System.gc()
            }*/
        }
    }


    private fun calc(): Double {
        val x = Random.nextInt(30) + 10
        val y = Random.nextInt(30) + 10
        val z = Random.nextInt(30) + 10
        val a = Random.nextFloat()
        val b = Random.nextFloat()
        val c = Random.nextFloat()
        return (a + x).pow((b + y)) / tan((a + x).toDouble() / (b + y)) +
                (c + z).pow((a + x)) / tan((b + z).toDouble() / (c + x))
    }

}