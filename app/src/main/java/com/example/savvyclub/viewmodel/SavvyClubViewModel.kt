package com.example.savvyclub.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.example.savvyclub.model.Puzzle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SavvyClubViewModel(
    private val context: Context,
    private val allPuzzles: List<Puzzle>
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("savvy_prefs", Context.MODE_PRIVATE)

    private val _currentPuzzle = MutableStateFlow<Puzzle?>(null)
    val currentPuzzle: StateFlow<Puzzle?> = _currentPuzzle

    private val _showAnswer = MutableStateFlow(false)
    val showAnswer: StateFlow<Boolean> = _showAnswer

    // Храним ID просмотренных ответов как строки
    private var viewedAnswers: MutableSet<String> =
        prefs.getStringSet("viewed_answers", emptySet())?.toMutableSet() ?: mutableSetOf()

    private var availablePuzzles: List<Puzzle> = filterAvailablePuzzles()

    private var currentIndex = 0

    init {
        updateCurrentPuzzle()
    }

    private fun filterAvailablePuzzles(): List<Puzzle> {
        return allPuzzles.filter { it.id.toString() !in viewedAnswers }
    }

    private fun updateCurrentPuzzle() {
        if (availablePuzzles.isNotEmpty()) {
            // Защита от выхода за пределы
            if (currentIndex >= availablePuzzles.size) currentIndex = 0
            _currentPuzzle.value = availablePuzzles[currentIndex]
        } else {
            _currentPuzzle.value = null
        }
        _showAnswer.value = false
    }

    fun toggleAnswer() {
        val puzzle = _currentPuzzle.value ?: return
        val currentShow = _showAnswer.value

        if (!currentShow) {
            // Помечаем ответ как просмотренный
            viewedAnswers.add(puzzle.id.toString())
            prefs.edit().putStringSet("viewed_answers", viewedAnswers).apply()

            availablePuzzles = filterAvailablePuzzles()

            // Если список стал пустым — currentIndex в 0, иначе корректируем currentIndex
            currentIndex = if (availablePuzzles.isEmpty()) 0 else currentIndex.coerceAtMost(availablePuzzles.size - 1)
        }

        _showAnswer.value = !currentShow
    }

    fun nextPuzzle() {
        val size = availablePuzzles.size
        if (size == 0) return
        currentIndex = (currentIndex + 1) % size
        updateCurrentPuzzle()
    }

    fun prevPuzzle() {
        val size = availablePuzzles.size
        if (size == 0) return
        currentIndex = if (currentIndex - 1 < 0) size - 1 else currentIndex - 1
        updateCurrentPuzzle()
    }

    fun resetProgress() {
        viewedAnswers.clear()
        prefs.edit().remove("viewed_answers").apply()
        availablePuzzles = allPuzzles
        currentIndex = 0
        updateCurrentPuzzle()
    }
}

