package com.example.savvyclub.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Puzzle(
    val id: Int,
    val type: String,
    val q: String,
    val a: String,
    val question: Map<String, String>,  // Например, {"ru": "вопрос", "en": "question"}
    val answer: Map<String, String>     // Например, {"ru": "ответ", "en": "answer"}
)

