package com.example.savvyclub.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Puzzle(
    val id: Int,
    val q: String,
    val a: String,
    val localization: Map<String, List<String>>
)
