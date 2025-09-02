package com.example.savvyclub.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.example.savvyclub.data.model.PuzzleManifest
import com.example.savvyclub.data.util.FileHashUtils.calculateMD5
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.*
import java.util.zip.ZipInputStream

object PuzzleUpdateManager {
    private const val TAG = "PuzzleUpdateManager"
    private const val PREFS = "puzzle_update_prefs"
    private const val PREF_LATEST_VERSION = "latest_manifest_version"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
    }

    /**
     * Скачивает и устанавливает пакет, если версия манифеста новее.
     * Возвращает имя папки пакета (manifest.folder), либо null если обновления нет/ошибка.
     */
    suspend fun checkForUpdatesWithPackage(
        context: Context,
        manifestUrl: String,
        onDownloadProgress: ((Float) -> Unit)? = null,
        onUnpackProgress: ((Float) -> Unit)? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            // 1) читаем манифест
            val responseText = client.get(manifestUrl).bodyAsText()
            Log.d(TAG, "Manifest raw: $responseText")
            val manifest = json.decodeFromString<PuzzleManifest>(responseText)

            // 2) сравнение версий + проверка наличия папки
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val latest = prefs.getInt(PREF_LATEST_VERSION, 0)
            val targetFolder = File(context.filesDir, "puzzles/${manifest.folder}")

            if (manifest.version <= latest && targetFolder.exists()) {
                Log.i(TAG, "No update needed. Remote=${manifest.version} Local=$latest")
                return@withContext null
            }

            // 3) качаем zip во временный файл
            val tmpZip = File.createTempFile("puzzles_", ".zip", context.cacheDir)
            downloadFile(manifest.url, tmpZip, onDownloadProgress)

            // 4) проверка MD5
            val md5 = calculateMD5(tmpZip)
            if (!md5.equals(manifest.md5, ignoreCase = true)) {
                Log.e(TAG, "MD5 mismatch: local=$md5, expected=${manifest.md5}")
                tmpZip.delete()
                return@withContext null
            } else {
                Log.i(TAG, "MD5 OK: $md5")
            }

            // 5) распаковываем
            if (targetFolder.exists()) {
                targetFolder.deleteRecursively()
            }
            targetFolder.mkdirs()

            unzip(tmpZip, targetFolder, onUnpackProgress)
            logUnpackedFiles(targetFolder)

            // 6) сохраняем последнюю версию
            prefs.edit { putInt(PREF_LATEST_VERSION, manifest.version) }

            tmpZip.delete()
            return@withContext manifest.folder
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }


    private suspend fun downloadFile(
        url: String,
        dest: File,
        onProgress: ((Float) -> Unit)?
    ) = withContext(Dispatchers.IO) {
        dest.parentFile?.mkdirs()
        val resp: HttpResponse = client.get(url)
        val bytes = resp.body<ByteArray>()
        val total = bytes.size.toFloat().coerceAtLeast(1f)
        var copied = 0

        FileOutputStream(dest).use { fos ->
            val buf = ByteArray(64 * 1024)
            var off = 0
            while (off < bytes.size) {
                val len = minOf(buf.size, bytes.size - off)
                fos.write(bytes, off, len)
                off += len
                copied += len
                onProgress?.invoke(copied / total)
            }
            fos.flush()
        }
    }

    private fun unzip(zipFile: File, targetDir: File, onProgress: ((Float) -> Unit)?) {
        if (!targetDir.exists()) targetDir.mkdirs()

        val totalEntries = countZipEntries(zipFile).coerceAtLeast(1)
        var processed = 0

        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val out = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { fos -> zis.copyTo(fos) }
                }
                processed++
                onProgress?.invoke(processed.toFloat() / totalEntries)
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun countZipEntries(zipFile: File): Int {
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var count = 0
            var e = zis.nextEntry
            while (e != null) {
                count++
                e = zis.nextEntry
            }
            return count
        }
    }

    private fun logUnpackedFiles(targetDir: File) {
        if (!targetDir.exists()) return
        targetDir.walkTopDown().forEach { f ->
            Log.d(TAG, "Unpacked: ${f.relativeTo(targetDir)}")
        }
    }
}
