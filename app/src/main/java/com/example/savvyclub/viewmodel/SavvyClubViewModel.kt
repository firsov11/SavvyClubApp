package com.example.savvyclub.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savvyclub.data.PuzzleRepository
import com.example.savvyclub.data.PuzzleUpdateManager
import com.example.savvyclub.data.model.PuzzleItem
import com.example.savvyclub.data.model.PuzzleSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class SavvyClubViewModel(
    private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("puzzle_prefs", Context.MODE_PRIVATE)
    private val KEY_INDEX = "current_index"
    private val KEY_SHOW_ANSWER = "show_answer"
    private val KEY_PACKAGE = "current_package"  // Используем эту константу

    private val _puzzles = MutableStateFlow<List<PuzzleItem>>(emptyList())
    val puzzles: StateFlow<List<PuzzleItem>> = _puzzles.asStateFlow()

    private val _currentIndex = MutableStateFlow(prefs.getInt(KEY_INDEX, 0))
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _showAnswer = MutableStateFlow(prefs.getBoolean(KEY_SHOW_ANSWER, false))
    val showAnswer: StateFlow<Boolean> = _showAnswer.asStateFlow()

    private val _currentPuzzle = MutableStateFlow<PuzzleItem?>(null)
    val currentPuzzle: StateFlow<PuzzleItem?> = _currentPuzzle.asStateFlow()

    private val _currentPackage = MutableStateFlow(prefs.getString(KEY_PACKAGE, "package_default") ?: "package_default")
    val currentPackage: StateFlow<String> = _currentPackage.asStateFlow()

    init {
        loadPuzzles()
        checkForUpdates()
    }

    private fun loadPuzzles() {
        viewModelScope.launch {
            val all = PuzzleRepository.loadAll(context) // Возвращает List<PuzzleItem>
            _puzzles.value = all

            val idx = _currentIndex.value
            if (idx !in all.indices) {
                _currentIndex.value = 0
                saveProgress(0, false)
            }

            updateCurrentPuzzle()
        }
    }

    private fun updateCurrentPuzzle() {
        _currentPuzzle.value = _puzzles.value.getOrNull(_currentIndex.value)
    }

    fun nextPuzzle() {
        if (_puzzles.value.isNotEmpty()) {
            val next = (_currentIndex.value + 1) % _puzzles.value.size
            _currentIndex.value = next
            _showAnswer.value = false
            saveProgress(next, false)
            updateCurrentPuzzle()
        }
    }

    fun prevPuzzle() {
        if (_puzzles.value.isNotEmpty()) {
            val prev = (_currentIndex.value - 1 + _puzzles.value.size) % _puzzles.value.size
            _currentIndex.value = prev
            _showAnswer.value = false
            saveProgress(prev, false)
            updateCurrentPuzzle()
        }
    }

    fun toggleAnswer() {
        val newShow = !_showAnswer.value
        _showAnswer.value = newShow
        saveProgress(_currentIndex.value, newShow)
    }

    fun resetProgress() {
        _currentIndex.value = 0
        _showAnswer.value = false
        saveProgress(0, false)
        updateCurrentPuzzle()
    }

    private fun saveProgress(index: Int, showAnswer: Boolean) {
        prefs.edit().apply {
            putInt(KEY_INDEX, index)
            putBoolean(KEY_SHOW_ANSWER, showAnswer)
        }.apply()
    }

    private fun saveCurrentPackage(packageName: String) {
        prefs.edit().putString(KEY_PACKAGE, packageName).apply()
        _currentPackage.value = packageName
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            try {
                val puzzlesDir = File(context.filesDir, "puzzles")
                val manifestUrl = "https://drive.google.com/uc?export=download&id=18WfPI-rqRNeQbv1BdRU3to919EzYS0cJ"

                val updateResult = PuzzleUpdateManager.checkForUpdatesWithPackage(context, manifestUrl, puzzlesDir)

                if (updateResult != null && updateResult.puzzles.isNotEmpty()) {
                    val updatedPuzzleItems = updateResult.puzzles.map { puzzle ->
                        PuzzleItem(puzzle = puzzle, source = PuzzleSource.LOCAL)
                    }

                    _puzzles.value = updatedPuzzleItems
                    _currentIndex.value = 0
                    _showAnswer.value = false
                    saveProgress(0, false)

                    saveCurrentPackage(updateResult.packageName)

                    updateCurrentPuzzle()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
