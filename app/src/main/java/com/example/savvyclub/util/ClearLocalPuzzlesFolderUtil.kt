package com.example.savvyclub.util

import android.content.Context
import java.io.File

fun clearLocalPuzzlesFolder(context: Context) {
    val puzzlesDir = File(context.filesDir, "puzzles")
    if (puzzlesDir.exists() && puzzlesDir.isDirectory) {
        puzzlesDir.deleteRecursively()
        puzzlesDir.mkdirs()
    }
}
