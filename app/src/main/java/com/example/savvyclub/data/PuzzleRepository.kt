package com.example.savvyclub.data

import android.content.Context
import com.example.savvyclub.data.model.Puzzle
import kotlinx.serialization.json.Json

fun loadPuzzlesFromAssets(context: Context): List<Puzzle> {
    val jsonString = context.assets.open("puzzles.json")
        .bufferedReader()
        .use { it.readText() }

    return Json.decodeFromString(jsonString)
}

