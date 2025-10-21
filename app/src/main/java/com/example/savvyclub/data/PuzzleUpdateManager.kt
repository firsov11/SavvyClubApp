package com.example.savvyclub.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.example.savvyclub.data.util.FileHashUtils.calculateMD5
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.regex.Pattern
import java.util.zip.ZipInputStream

object PuzzleUpdateManager {
    private const val TAG = "PuzzleUpdateManager"
    private const val PREFS = "puzzle_update_prefs"
    private const val PREF_HASH_PREFIX = "puzzle_hash_"

    private val client = HttpClient(OkHttp)

    /**
     * Устанавливает пакет (скачивает и распаковывает) с проверкой MD5
     * Повторяет до 3 раз при несовпадении хэша
     */
    suspend fun ensurePackageInstalled(
        context: Context,
        packageId: String,
        fileId: String,
        expectedHash: String
    ): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "ensurePackageInstalled called for $packageId")

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val targetDir = File(context.filesDir, "puzzles/$packageId")
        val zipFile = File(context.cacheDir, "$packageId.zip")

        repeat(3) { attempt ->
            try {
                val savedHash = prefs.getString("$PREF_HASH_PREFIX$packageId", null)

                if (targetDir.exists() && expectedHash.equals(savedHash, ignoreCase = true)) {
                    Log.i(TAG, "Package $packageId is already up-to-date ($expectedHash)")
                    return@withContext false
                }

                Log.i(TAG, "Downloading $packageId (attempt ${attempt + 1})")
                downloadFromGoogleDrive(client, fileId, zipFile)

                val actualHash = calculateMD5(zipFile)
                if (!actualHash.equals(expectedHash, ignoreCase = true)) {
                    Log.e(TAG, "Hash mismatch for $packageId: $actualHash != $expectedHash")
                    zipFile.delete()
                    if (attempt == 2) {
                        Log.e(TAG, "Giving up after 3 failed attempts for $packageId")
                        return@withContext false
                    }
                    return@repeat
                }

                if (targetDir.exists()) targetDir.deleteRecursively()
                targetDir.mkdirs()
                unzip(zipFile, targetDir)
                zipFile.delete()

                prefs.edit { putString("$PREF_HASH_PREFIX$packageId", actualHash) }
                Log.i(TAG, "Package $packageId successfully installed.")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Attempt ${attempt + 1} failed for $packageId: ${e.message}", e)
                if (attempt == 2) return@withContext false
            }
        }

        false
    }

    /**
     * Скачивает файл с Google Drive, корректно обрабатывая confirm-токен для больших файлов
     */
    private suspend fun downloadFromGoogleDrive(
        client: HttpClient,
        fileId: String,
        dest: File
    ) = withContext(Dispatchers.IO) {
        dest.parentFile?.mkdirs()

        var url = "https://drive.google.com/uc?export=download&id=$fileId"
        var response: HttpResponse = client.get(url)

        val contentType = response.headers["Content-Type"] ?: ""
        if (contentType.contains("text/html")) {
            val html = response.bodyAsText()
            val matcher = Pattern.compile("confirm=([0-9A-Za-z_]+)").matcher(html)
            if (matcher.find()) {
                val confirmToken = matcher.group(1)
                url = "https://drive.google.com/uc?export=download&confirm=$confirmToken&id=$fileId"
                response = client.get(url)
            }
        }

        val channel: ByteReadChannel = response.body()
        FileOutputStream(dest).use { fos ->
            val buffer = ByteArray(8192)
            while (!channel.isClosedForRead) {
                val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                if (bytesRead == -1) break
                fos.write(buffer, 0, bytesRead)
            }
        }
    }

    /**
     * Распаковывает ZIP-архив в указанную директорию
     */
    private fun unzip(zipFile: File, targetDir: File) {
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (entry.isDirectory) outFile.mkdirs()
                else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
