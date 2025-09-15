package com.example.savvyclub.data.util

import java.io.File
import java.security.MessageDigest

object FileHashUtils {
    fun calculateMD5(file: File): String {
        val buffer = ByteArray(1024 * 1024)
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { fis ->
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}