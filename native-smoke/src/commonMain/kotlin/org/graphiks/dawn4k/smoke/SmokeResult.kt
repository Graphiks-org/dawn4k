package org.graphiks.dawn4k.smoke

/**
 * JSON protocol emitted by the native smoke runner.
 *
 * Fields: target, revision, backend, adapterDescription, callbackCount, bufferSize,
 * status ("passed" or "failed"), diagnostic.
 */
data class SmokeResult(
    val target: String,
    val revision: String,
    val backend: String,
    val adapterDescription: String,
    val callbackCount: Int,
    val bufferSize: Long,
    val status: String,
    val diagnostic: String,
) {
    fun toJson(): String = buildString {
        append('{')
        appendField("target", target, first = true)
        appendField("revision", revision)
        appendField("backend", backend)
        appendField("adapterDescription", adapterDescription)
        appendField("callbackCount", callbackCount.toString())
        appendField("bufferSize", bufferSize.toString())
        appendField("status", status)
        appendField("diagnostic", diagnostic)
        append('}')
    }

    private fun StringBuilder.appendField(name: String, value: String, first: Boolean = false) {
        if (!first) append(',')
        append('"').append(name).append("\":\"").append(escape(value)).append('"')
    }

    private fun escape(value: String): String = buildString {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 0x20) {
                    append("\\u").append(character.code.toString(16).padStart(4, '0'))
                } else {
                    append(character)
                }
            }
        }
    }
}
