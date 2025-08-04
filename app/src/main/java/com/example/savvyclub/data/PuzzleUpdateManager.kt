package com.example.savvyclub.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.example.savvyclub.data.model.Puzzle
import com.example.savvyclub.data.model.PuzzleManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.*
import java.util.zip.ZipInputStream
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.*

object PuzzleUpdateManager {
    private const val PREFS_NAME = "puzzle_prefs"
    private const val PREF_VERSION = "puzzle_version"
    private const val TAG = "PuzzleUpdateManager"

    val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
    }

    data class UpdateResult(
        val puzzles: List<Puzzle>,
        val packageName: String
    )

    suspend fun checkForUpdatesWithPackage(
        context: Context,
        manifestUrl: String,
        puzzlesDir: File
    ): UpdateResult? = withContext(Dispatchers.IO) {
        try {
            val responseText = client.get(manifestUrl).bodyAsText()
            val manifest = json.decodeFromString<PuzzleManifest>(responseText)

            // Создаём папку для головоломок, если надо
            if (!puzzlesDir.exists()) puzzlesDir.mkdirs()

            // Файл архива, который будем скачивать
            val zipFile = File(puzzlesDir, manifest.filename)

            // Скачиваем архив (заменяем всегда)
            downloadFile(manifest.url, zipFile)

            // Папка для распаковки, например "package_5"
            val targetFolder = File(puzzlesDir, manifest.folder)

            // Распаковываем в targetFolder
            unzip(zipFile, targetFolder)

            // Сохраняем версию (опционально)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit { putInt(PREF_VERSION, manifest.version) }

            // Читаем puzzles.json из папки targetFolder
            val puzzlesJson = File(targetFolder, "puzzles.json")
            if (puzzlesJson.exists()) {
                val jsonStr = puzzlesJson.readText()
                val puzzles: List<Puzzle> = json.decodeFromString(jsonStr)
                UpdateResult(puzzles = puzzles, packageName = manifest.folder)
            } else {
                Log.e(TAG, "puzzles.json not found in ${targetFolder.path}")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadFile(url: String, destFile: File) {
        destFile.parentFile?.let {
            if (!it.exists()) it.mkdirs()
        }

        val response = client.get(url)
        val bytes = response.body<ByteArray>()
        destFile.writeBytes(bytes)
        Log.i(TAG, "File downloaded to ${destFile.absolutePath}")
    }

    fun unzip(zipFile: File, targetDir: File) {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
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
