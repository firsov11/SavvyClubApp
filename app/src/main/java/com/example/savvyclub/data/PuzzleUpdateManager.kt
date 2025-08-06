package com.example.savvyclub.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.example.savvyclub.data.model.Puzzle
import com.example.savvyclub.data.model.PuzzleManifest
import com.example.savvyclub.data.model.UpdateResult
import com.example.savvyclub.data.util.FileHashUtils.calculateMD5
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.*
import java.util.zip.ZipInputStream

object PuzzleUpdateManager {
    private const val PREFS_NAME = "puzzle_prefs"
    private const val PREF_VERSION = "puzzle_version"
    private const val TAG = "PuzzleUpdateManager"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
    }

    suspend fun checkForUpdatesWithPackage(
        context: Context,
        manifestUrl: String,
        puzzlesDir: File
    ): UpdateResult? = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentVersion = prefs.getInt(PREF_VERSION, 0)

            // Загружаем manifest.json
            val responseText = client.get(manifestUrl).bodyAsText()
            val manifest = json.decodeFromString<PuzzleManifest>(responseText)

            if (!puzzlesDir.exists()) puzzlesDir.mkdirs()

            val zipFile = File(puzzlesDir, manifest.filename)

            var fileIsValid = false

            // Проверка MD5-хэша, если файл уже существует
            if (zipFile.exists()) {
                val localMd5 = calculateMD5(zipFile)
                if (localMd5.equals(manifest.md5, ignoreCase = true)) {
                    Log.i(TAG, "ZIP file MD5 matches.")
                    fileIsValid = true
                } else {
                    Log.w(TAG, "ZIP file MD5 mismatch. Will re-download.")
                }
            }

            // Условия когда НЕ нужно скачивать файл заново
            if (manifest.version <= currentVersion && fileIsValid) {
                Log.i(TAG, "Package version and MD5 are up to date. Skipping download.")
                return@withContext null
            }

            // Скачиваем ZIP, если:
            // 1. Версия новее
            // 2. Локальный файл невалидный
            Log.i(TAG, "Downloading package: ${manifest.filename}")
            downloadFile(manifest.url, zipFile)

            // Распаковываем
            val targetFolder = File(puzzlesDir, manifest.folder)
            unzip(zipFile, targetFolder)

            // Сохраняем новую версию
            prefs.edit { putInt(PREF_VERSION, manifest.version) }

            // Чтение puzzles.json
            val puzzlesJson = File(targetFolder, "puzzles.json")
            if (puzzlesJson.exists()) {
                val jsonStr = puzzlesJson.readText()
                val puzzles: List<Puzzle> = json.decodeFromString(jsonStr)
                return@withContext UpdateResult(puzzles = puzzles, packageName = manifest.folder)
            } else {
                Log.e(TAG, "puzzles.json not found in ${targetFolder.path}")
                return@withContext null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    private suspend fun downloadFile(url: String, destFile: File) {
        destFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
        val response = client.get(url)
        val bytes = response.body<ByteArray>()
        destFile.writeBytes(bytes)
        Log.i(TAG, "File downloaded to ${destFile.absolutePath}")
    }

    private fun unzip(zipFile: File, targetDir: File) {
        if (!targetDir.exists()) targetDir.mkdirs()
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        Log.i(TAG, "Unzip complete to ${targetDir.absolutePath}")
    }
}
