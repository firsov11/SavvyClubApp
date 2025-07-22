package com.example.savvyclub.model

import kotlinx.serialization.Serializable

@Serializable
data class Puzzle(
    val id: Int,   // уникальный ID для прогресса
    val q: String,    // путь к изображению вопроса (например, "q1.png")
    val a: String     // путь к изображению ответа (например, "a1.png")
)


