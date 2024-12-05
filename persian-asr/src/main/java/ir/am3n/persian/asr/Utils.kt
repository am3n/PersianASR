package ir.am3n.persian.asr

object Utils {

    fun<T: Any> T.accessField(fieldName: String): Any? {
        return javaClass.getDeclaredField(fieldName).let { field ->
            field.isAccessible = true
            return@let field.get(this)
        }
    }

}