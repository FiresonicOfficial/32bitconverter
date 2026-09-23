package com.example.compat

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Android Binary XML (AXML) Manifest String Pool Patcher.
 * Modifies package identifiers and authorities directly inside the compiled AndroidManifest.xml
 * to allow cloning and installing an APK as an independent application under a new package ID.
 */
object AxmlPackageRenamer {

    private const val RES_XML_TYPE = 0x00080003
    private const val RES_STRING_POOL_TYPE = 0x00010001
    private const val FLAG_IS_UTF8 = 1 shl 8

    /**
     * Renames the package attribute and related authority/permission strings in binary AndroidManifest.xml
     */
    fun renamePackage(
        originalManifestBytes: ByteArray,
        oldPackageName: String,
        newPackageName: String,
        oldAppName: String? = null,
        newAppName: String? = null
    ): ByteArray {
        if (originalManifestBytes.size < 36) return originalManifestBytes

        val buffer = ByteBuffer.wrap(originalManifestBytes).order(ByteOrder.LITTLE_ENDIAN)

        val xmlMagic = buffer.int
        if (xmlMagic != RES_XML_TYPE && xmlMagic != 0x00030008) {
            // Not a recognized binary XML
            return originalManifestBytes
        }

        val originalXmlSize = buffer.int

        // String pool chunk header
        val poolStart = buffer.position()
        val poolType = buffer.int
        if (poolType != RES_STRING_POOL_TYPE && poolType != 0x00010001) {
            return originalManifestBytes
        }

        val originalPoolSize = buffer.int
        val stringCount = buffer.int
        val styleCount = buffer.int
        val flags = buffer.int
        val stringsStartRelative = buffer.int
        val stylesStartRelative = buffer.int

        val isUtf8 = (flags and FLAG_IS_UTF8) != 0

        // Read string offsets
        val stringOffsets = IntArray(stringCount)
        for (i in 0 until stringCount) {
            stringOffsets[i] = buffer.int
        }

        // Style offsets (if any)
        val styleOffsets = IntArray(styleCount)
        for (i in 0 until styleCount) {
            styleOffsets[i] = buffer.int
        }

        val stringsAbsoluteStart = poolStart + stringsStartRelative
        val stylesAbsoluteStart = if (styleCount > 0 && stylesStartRelative > 0) {
            poolStart + stylesStartRelative
        } else {
            poolStart + originalPoolSize
        }

        // Parse all strings from the pool
        val stringList = mutableListOf<String>()
        for (i in 0 until stringCount) {
            val strOffset = stringsAbsoluteStart + stringOffsets[i]
            buffer.position(strOffset)

            if (isUtf8) {
                // UTF-8 string format:
                // [charCount: 1 or 2 bytes] [byteCount: 1 or 2 bytes] [bytes] [0x00]
                var charLen = buffer.get().toInt() and 0xFF
                if ((charLen and 0x80) != 0) {
                    charLen = ((charLen and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF)
                }

                var byteLen = buffer.get().toInt() and 0xFF
                if ((byteLen and 0x80) != 0) {
                    byteLen = ((byteLen and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF)
                }

                val strBytes = ByteArray(byteLen)
                buffer.get(strBytes)
                stringList.add(String(strBytes, Charsets.UTF_8))
            } else {
                // UTF-16 string format:
                // [charCount: 2 or 4 bytes] [charCount * 2 bytes UTF-16LE] [0x00, 0x00]
                var charCount = buffer.short.toInt() and 0xFFFF
                if ((charCount and 0x8000) != 0) {
                    charCount = ((charCount and 0x7FFF) shl 16) or (buffer.short.toInt() and 0xFFFF)
                }

                val strBytes = ByteArray(charCount * 2)
                buffer.get(strBytes)
                stringList.add(String(strBytes, Charsets.UTF_16LE))
            }
        }

        // Extract style bytes if present
        val styleBytes = if (styleCount > 0 && stylesAbsoluteStart < originalManifestBytes.size) {
            val styleLen = (poolStart + originalPoolSize) - stylesAbsoluteStart
            if (styleLen > 0) {
                originalManifestBytes.copyOfRange(stylesAbsoluteStart, stylesAbsoluteStart + styleLen)
            } else {
                ByteArray(0)
            }
        } else {
            ByteArray(0)
        }

        // Replace strings
        var modifiedAny = false
        val newStringList = stringList.map { originalStr ->
            when {
                originalStr == oldPackageName -> {
                    modifiedAny = true
                    newPackageName
                }
                oldAppName != null && newAppName != null && originalStr == oldAppName -> {
                    modifiedAny = true
                    newAppName
                }
                originalStr.startsWith("$oldPackageName.") -> {
                    // Replace package prefix in authorities, permissions, or providers
                    if (originalStr.contains("provider") || originalStr.contains("permission") || originalStr.contains("authority") || originalStr.contains("fileprovider")) {
                        modifiedAny = true
                        originalStr.replaceFirst(oldPackageName, newPackageName)
                    } else {
                        originalStr
                    }
                }
                else -> originalStr
            }
        }

        if (!modifiedAny) {
            // Nothing to modify
            return originalManifestBytes
        }

        // Re-encode string pool data
        val newStringDataStream = ByteArrayOutputStream()
        val newOffsets = IntArray(stringCount)

        for (i in 0 until stringCount) {
            newOffsets[i] = newStringDataStream.size()
            val str = newStringList[i]

            if (isUtf8) {
                val utf8Bytes = str.toByteArray(Charsets.UTF_8)
                val charLen = str.length
                val byteLen = utf8Bytes.size

                // Write char count
                if (charLen <= 0x7F) {
                    newStringDataStream.write(charLen)
                } else {
                    newStringDataStream.write(((charLen shr 8) and 0x7F) or 0x80)
                    newStringDataStream.write(charLen and 0xFF)
                }

                // Write byte count
                if (byteLen <= 0x7F) {
                    newStringDataStream.write(byteLen)
                } else {
                    newStringDataStream.write(((byteLen shr 8) and 0x7F) or 0x80)
                    newStringDataStream.write(byteLen and 0xFF)
                }

                newStringDataStream.write(utf8Bytes)
                newStringDataStream.write(0) // Null terminator
            } else {
                val utf16Bytes = str.toByteArray(Charsets.UTF_16LE)
                val charCount = str.length

                if (charCount <= 0x7FFF) {
                    newStringDataStream.write(charCount and 0xFF)
                    newStringDataStream.write((charCount shr 8) and 0xFF)
                } else {
                    val high = ((charCount shr 16) and 0x7FFF) or 0x8000
                    newStringDataStream.write(high and 0xFF)
                    newStringDataStream.write((high shr 8) and 0xFF)
                    newStringDataStream.write(charCount and 0xFF)
                    newStringDataStream.write((charCount shr 8) and 0xFF)
                }

                newStringDataStream.write(utf16Bytes)
                newStringDataStream.write(0) // 2 null terminator bytes
                newStringDataStream.write(0)
            }
        }

        val rawStringData = newStringDataStream.toByteArray()
        val stringPadding = (4 - (rawStringData.size % 4)) % 4
        val paddedStringDataSize = rawStringData.size + stringPadding

        val newStringsStart = 28 + (stringCount * 4) + (styleCount * 4)
        val newStylesStart = if (styleCount > 0) newStringsStart + paddedStringDataSize else 0
        val stylePadding = (4 - (styleBytes.size % 4)) % 4
        val paddedStyleDataSize = styleBytes.size + stylePadding

        val newPoolSize = newStringsStart + paddedStringDataSize + paddedStyleDataSize

        val afterPoolOffset = poolStart + originalPoolSize
        val remainingBytesLength = originalManifestBytes.size - afterPoolOffset
        val newTotalXmlSize = poolStart + newPoolSize + remainingBytesLength

        val resultStream = ByteArrayOutputStream(newTotalXmlSize)
        val headerBuf = ByteBuffer.allocate(poolStart + 28 + (stringCount * 4) + (styleCount * 4)).order(ByteOrder.LITTLE_ENDIAN)

        // Write XML Header
        headerBuf.putInt(xmlMagic)
        headerBuf.putInt(newTotalXmlSize)

        // Write String Pool Header
        headerBuf.putInt(poolType)
        headerBuf.putInt(newPoolSize)
        headerBuf.putInt(stringCount)
        headerBuf.putInt(styleCount)
        headerBuf.putInt(flags)
        headerBuf.putInt(newStringsStart)
        headerBuf.putInt(newStylesStart)

        // Write String Offsets
        for (offset in newOffsets) {
            headerBuf.putInt(offset)
        }

        // Write Style Offsets
        for (sOffset in styleOffsets) {
            headerBuf.putInt(sOffset)
        }

        resultStream.write(headerBuf.array())
        resultStream.write(rawStringData)

        // Padding for strings
        for (p in 0 until stringPadding) {
            resultStream.write(0)
        }

        // Write Styles
        if (styleBytes.isNotEmpty()) {
            resultStream.write(styleBytes)
            for (p in 0 until stylePadding) {
                resultStream.write(0)
            }
        }

        // Write remaining XML chunks (Resource map, Start/End elements, etc.)
        if (remainingBytesLength > 0) {
            resultStream.write(originalManifestBytes, afterPoolOffset, remainingBytesLength)
        }

        return resultStream.toByteArray()
    }
}
