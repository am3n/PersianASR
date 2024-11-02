package ir.am3n.persian.asr

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class Partial(
    @SerializedName("partial")
    val text: String
) {
    internal val asResult: Result
        get() = Result(
            listOf(
                Result.Data(
                    text = text,
                    confidence = 0f
                )
            )
        )
}
