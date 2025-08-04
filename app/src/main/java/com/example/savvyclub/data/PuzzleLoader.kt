package com.example.savvyclub.data

import android.content.Context
import android.util.Log
import com.example.savvyclub.data.model.Puzzle
import com.example.savvyclub.data.model.PuzzleItem
import com.example.savvyclub.data.model.PuzzleSource
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PuzzleLoader {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadAllPuzzlesWithSource(context: Context): List<PuzzleItem> = withContext(Dispatchers.IO) {
        val allPuzzles = mutableListOf<PuzzleItem>()

        // 1. Загрузим из assets/puzzles.json
        val assetsPuzzles = loadFromAssets(context)
        Log.i("PuzzleLoader", "Loaded ${assetsPuzzles.size} puzzles from assets")
        allPuzzles += assetsPuzzles.map { PuzzleItem(it, PuzzleSource.ASSETS) }

        // 2. Загрузим из распакованных локальных директорий
        val localPuzzleDir = File(context.filesDir, "puzzles")
        Log.i("PuzzleLoader", "Currently total puzzles (assets only): ${allPuzzles.size}")
        if (localPuzzleDir.exists()) {
            val folders = localPuzzleDir.listFiles { file -> file.isDirectory }
            Log.i("PuzzleLoader", "Folders in local dir: ${folders?.joinToString { it.name } ?: "none"}")

            folders?.forEach { folder ->
                val puzzlesJson = File(folder, "puzzles.json")
                if (puzzlesJson.exists()) {
                    try {
                        val jsonStr = puzzlesJson.readText()
                        val puzzles = json.decodeFromString<List<Puzzle>>(jsonStr)
                        Log.i("PuzzleLoader", "Loaded ${puzzles.size} puzzles from ${folder.name}")
                        allPuzzles += puzzles.map { PuzzleItem(it, PuzzleSource.LOCAL) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        Log.i("PuzzleLoader", "Loaded total puzzles (assets + local): ${allPuzzles.size}")

        return@withContext allPuzzles
    }

    private fun loadFromAssets(context: Context): List<Puzzle> {
        return try {
            val input = context.assets.open("puzzles.json")
            val text = input.bufferedReader().use { it.readText() }
            json.decodeFromString(text)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
