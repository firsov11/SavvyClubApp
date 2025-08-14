package com.example.savvyclub.data

import android.content.Context
import android.util.Log
import com.example.savvyclub.data.model.Puzzle
import com.example.savvyclub.data.model.PuzzleItem
import com.example.savvyclub.data.model.PuzzleSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

object PuzzleLoader {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun loadAllPuzzlesWithSource(context: Context): List<PuzzleItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<PuzzleItem>()
        result += loadAssetsPuzzles(context)
        result += loadLocalPuzzles(context)
        Log.i("PuzzleLoader", "Loaded total puzzles (assets + local): ${result.size}")
        result
    }

    suspend fun loadAssetsPuzzles(context: Context): List<PuzzleItem> = withContext(Dispatchers.IO) {
        val puzzles = try {
            context.assets.open("puzzles.json").use { it.bufferedReader().readText() }
                .let { json.decodeFromString<List<Puzzle>>(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
        Log.i("PuzzleLoader", "Loaded ${puzzles.size} puzzles from assets")
        puzzles.map { PuzzleItem(it, PuzzleSource.ASSETS) }
    }

    suspend fun loadLocalPuzzles(context: Context): List<PuzzleItem> = withContext(Dispatchers.IO) {
        val base = File(context.filesDir, "puzzles")
        if (!base.exists()) return@withContext emptyList()

        val items = mutableListOf<PuzzleItem>()
        val folders = base.listFiles { f -> f.isDirectory } ?: emptyArray()
        Log.i("PuzzleLoader", "Local packages: ${folders.joinToString { it.name }}")

        for (folder in folders) {
            items += loadFromPackage(context, folder.name)
        }
        items
    }

    /** Загружает конкретный пакет из подпапки filesDir/puzzles/<packageFolder> */
    suspend fun loadFromPackage(context: Context, packageFolder: String): List<PuzzleItem> = withContext(Dispatchers.IO) {
        val folder = File(context.filesDir, "puzzles/$packageFolder")
        val puzzlesJson = File(folder, "puzzles.json")
        if (!puzzlesJson.exists()) return@withContext emptyList()

        return@withContext try {
            val list = json.decodeFromString<List<Puzzle>>(puzzlesJson.readText())
            Log.i("PuzzleLoader", "Loaded ${list.size} puzzles from $packageFolder")
            // важно: префиксуем пути картинок именем пакета
            list.map { it.withPrefixedImages(packageFolder) }
                .map { PuzzleItem(it, PuzzleSource.LOCAL) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /** Если путь не содержит '/', добавляем '<packageFolder>/' */
    private fun Puzzle.withPrefixedImages(packageFolder: String): Puzzle {
        fun prefixIfNeeded(path: String): String {
            if (path.isEmpty()) return path
            if (path.contains("/")) return path
            return "$packageFolder/$path"
        }
        return copy(
            q = prefixIfNeeded(q),
            a = prefixIfNeeded(a)
        )
    }
}
