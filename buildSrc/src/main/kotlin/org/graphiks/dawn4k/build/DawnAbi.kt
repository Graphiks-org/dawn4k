package org.graphiks.dawn4k.build

/** A single type layout measured either from C or from the generated bindings. */
data class AbiType(
    val size: Long,
    val align: Long,
    val offsets: Map<String, Long>,
) {
    val offsetValues: Set<Long> get() = offsets.values.toSet()
}

/** A full layout dump: pointer size plus every measured type. */
data class AbiDump(
    val pointerSize: Int,
    val types: Map<String, AbiType>,
)

/**
 * Parsers and comparator for the dawn ABI oracle.
 *
 * The C side prints stable `key=value` lines (`T.size`, `T.align`, `T.field.offset`,
 * `pointerSize`). The generated side is read from the JVM registrations emitted by
 * kextract, which bake the exact size, alignment and field offsets into
 * `registerStructLayout` calls.
 */
object DawnAbi {

    fun parseOracleOutput(text: String): AbiDump {
        var pointerSize = 8
        val sizes = linkedMapOf<String, Long>()
        val aligns = linkedMapOf<String, Long>()
        val offsetsByType = linkedMapOf<String, MutableMap<String, Long>>()

        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val separator = line.indexOf('=')
            if (separator <= 0) continue
            val key = line.substring(0, separator)
            val value = line.substring(separator + 1).trim().toLongOrNull() ?: continue
            when {
                key == "pointerSize" -> pointerSize = value.toInt()
                key.endsWith(".size") -> sizes[key.removeSuffix(".size")] = value
                key.endsWith(".align") -> aligns[key.removeSuffix(".align")] = value
                key.endsWith(".offset") -> {
                    val path = key.removeSuffix(".offset")
                    val typeName = path.substringBefore('.')
                    val fieldName = path.substringAfter('.')
                    offsetsByType.getOrPut(typeName) { linkedMapOf() }[fieldName] = value
                }
            }
        }

        val types = linkedMapOf<String, AbiType>()
        val names = (sizes.keys + aligns.keys + offsetsByType.keys)
        for (name in names) {
            types[name] = AbiType(
                size = sizes[name] ?: continue,
                align = aligns[name] ?: continue,
                offsets = offsetsByType[name] ?: linkedMapOf(),
            )
        }
        return AbiDump(pointerSize, types)
    }

    fun parseGeneratedJvmAbi(source: String): AbiDump {
        val chunkFields = mutableMapOf<String, MutableList<Pair<String, Long>>>()
        val types = linkedMapOf<String, AbiType>()

        var currentChunk: String? = null
        var pendingName: String? = null
        var pendingSize: Long? = null
        var pendingAlign: Long? = null
        val pendingChunks = mutableListOf<String>()
        var inRegistration = false

        val chunkPattern = Regex("""^private fun (__kffiJvmStructFields\w+)\(\)""")
        val fieldPattern = Regex("""StructField\("([^"]*)",\s*[^,]+,\s*(\d+)L\)""")
        val namePattern = Regex("^\"([^\"]+)\",")
        val sizePattern = Regex("^(\\d+)L,\\s*(\\d+)L,")
        val chunkRefPattern = Regex("""(__kffiJvmStructFields\w+)\(\)""")

        for (raw in source.lineSequence()) {
            val line = raw.trim()
            val chunk = chunkPattern.find(line)
            if (chunk != null) {
                currentChunk = chunk.groupValues[1]
                chunkFields[currentChunk!!] = mutableListOf()
                continue
            }
            if (line.startsWith("private fun __kffiJvmRegisterStructLayout")) {
                currentChunk = null
                inRegistration = true
                pendingName = null
                pendingSize = null
                pendingAlign = null
                pendingChunks.clear()
                continue
            }
            val field = fieldPattern.find(line)
            if (field != null && currentChunk != null) {
                val name = field.groupValues[1]
                if (name != "__pad") {
                    chunkFields.getValue(currentChunk!!).add(name to field.groupValues[2].toLong())
                }
                continue
            }
            if (!inRegistration) continue

            val name = namePattern.find(line)
            if (name != null && pendingName == null) {
                pendingName = name.groupValues[1]
                continue
            }
            val size = sizePattern.find(line)
            if (size != null && pendingSize == null) {
                pendingSize = size.groupValues[1].toLong()
                pendingAlign = size.groupValues[2].toLong()
                continue
            }
            val refs = chunkRefPattern.findAll(line).map { it.groupValues[1] }.toList()
            if (refs.isNotEmpty()) {
                pendingChunks.addAll(refs)
                continue
            }
            if (line.startsWith(")")) {
                if (pendingName != null && pendingSize != null) {
                    val offsets = linkedMapOf<String, Long>()
                    var index = 0
                    pendingChunks.forEach { chunkName ->
                        chunkFields[chunkName]?.forEach { (fieldName, offset) ->
                            offsets["$fieldName#$index"] = offset
                            index++
                        }
                    }
                    types[pendingName!!] = AbiType(pendingSize!!, pendingAlign ?: 0L, offsets)
                }
                inRegistration = false
                pendingName = null
            }
        }
        return AbiDump(8, types)
    }

    /** Returns a human-readable list of every mismatch; empty means the ABI matches. */
    fun compare(oracle: AbiDump, generated: AbiDump): List<String> {
        val problems = mutableListOf<String>()
        if (oracle.pointerSize != generated.pointerSize) {
            problems += "pointerSize: C=${oracle.pointerSize} generated=${generated.pointerSize}"
        }
        for ((name, expected) in oracle.types) {
            val actual = generated.types[name]
            if (actual == null) {
                problems += "$name: missing from generated bindings"
                continue
            }
            if (expected.size != actual.size) {
                problems += "$name.size: C=${expected.size} generated=${actual.size}"
            }
            if (expected.align != actual.align) {
                problems += "$name.align: C=${expected.align} generated=${actual.align}"
            }
            if (expected.offsetValues != actual.offsetValues) {
                val missing = expected.offsetValues - actual.offsetValues
                val extra = actual.offsetValues - expected.offsetValues
                problems += "$name offsets differ: missing=$missing extra=$extra"
            }
            if (expected.offsets.size != actual.offsets.size) {
                problems += "$name: C has ${expected.offsets.size} fields, generated ${actual.offsets.size}"
            }
        }
        return problems
    }
}
