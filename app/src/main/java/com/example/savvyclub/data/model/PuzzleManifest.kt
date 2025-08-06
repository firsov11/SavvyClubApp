package com.example.savvyclub.data.model

@kotlinx.serialization.Serializable
data class PuzzleManifest(
    val version: Int,
    val url: String,         // Прямая ссылка на ZIP-файл
    val filename: String,    // Локальное имя архива
    val folder: String,      // Папка, в которую распаковать
    val md5: String
)
