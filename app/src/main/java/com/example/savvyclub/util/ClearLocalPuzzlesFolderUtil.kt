package com.example.savvyclub.util

import android.content.Context
import androidx.core.content.edit
import java.io.File

private const val PREFS = "puzzle_update_prefs"

fun clearLocalPuzzlesFolder(context: Context) {
    // Удаляем все папки с пазлами
    val puzzlesDir = File(context.filesDir, "puzzles")
    if (puzzlesDir.exists() && puzzlesDir.isDirectory) {
        puzzlesDir.deleteRecursively()
    }

    // Очищаем сохранённые хэши пакетов через KTX
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
        clear()
    }
}



