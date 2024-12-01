package ir.am3n.persian.asr

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class Result(
    @SerializedName("alternatives")
    val data: List<Data>
) {

    data class Data(
        @SerializedName("text")
        val text: String,
        @SerializedName("confidence")
        val confidence: Float
    )

    internal fun normalized(): Result {
        return Result(data.filter { it.text.isNotBlank() })
    }

    override fun toString(): String {
        return data.joinToString("\n")
    }

}