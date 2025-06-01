package ir.am3n.persian.asr

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class PartialDeserializer : JsonDeserializer<Partial> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Partial {
        val jsonObj = json!!.asJsonObject
        val text = jsonObj.get("partial").asString
        return Partial(text, System.currentTimeMillis())
    }
}
