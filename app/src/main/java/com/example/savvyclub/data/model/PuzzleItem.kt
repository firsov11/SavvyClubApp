package com.example.savvyclub.data.model

enum class PuzzleSource {
    ASSETS,
    LOCAL
}

data class PuzzleItem(
    val puzzle: Puzzle,
    val source: PuzzleSource
)
