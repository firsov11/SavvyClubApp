package com.example.savvyclub.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savvyclub.data.PuzzleLoader
import com.example.savvyclub.data.PuzzleUpdateManager
import com.example.savvyclub.data.model.PuzzleItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SavvyClubViewModel(
    private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("puzzle_prefs", Context.MODE_PRIVATE)

    private val KEY_INDEX = "current_index"
    private val KEY_SHOW_ANSWER = "show_answer"
    private val SOLVED_KEY = "solved_ids_set" // глобально; следи за уникальностью id в пакетах

    // UI state
    private val _puzzles = MutableStateFlow<List<PuzzleItem>>(emptyList())
    val puzzles: StateFlow<List<PuzzleItem>> = _puzzles.asStateFlow()

    private val _currentIndex = MutableStateFlow(prefs.getInt(KEY_INDEX, 0))
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _showAnswer = MutableStateFlow(prefs.getBoolean(KEY_SHOW_ANSWER, false))
    val showAnswer: StateFlow<Boolean> = _showAnswer.asStateFlow()

    private val _currentPuzzle = MutableStateFlow<PuzzleItem?>(null)
    val currentPuzzle: StateFlow<PuzzleItem?> = _currentPuzzle.asStateFlow()

    // progress
    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _unpackProgress = MutableStateFlow(0f)
    val unpackProgress: StateFlow<Float> = _unpackProgress.asStateFlow()

    init {
        // 1) мгновенно показываем всё что уже есть
        loadAllLocalAndAssets()

        // 2) в фоне проверяем обновления и, если есть, докидываем пазлы
        checkForUpdatesInBackground()
    }

    // ---------------- internal helpers ----------------

    private fun loadAllLocalAndAssets() {
        viewModelScope.launch {
            val all = PuzzleLoader.loadAllPuzzlesWithSource(context)
            _puzzles.value = all.filterNot { getSolvedIds().contains(it.puzzle.id) }
            if (_puzzles.value.isNotEmpty()) {
                _currentIndex.value = _currentIndex.value.coerceIn(0, _puzzles.value.lastIndex)
                _currentPuzzle.value = _puzzles.value.getOrNull(_currentIndex.value)
            } else {
                _currentIndex.value = 0
                _currentPuzzle.value = null
            }
        }
    }

    private fun addPackagePuzzles(packageFolder: String) {
        viewModelScope.launch {
            val newItems = PuzzleLoader.loadFromPackage(context, packageFolder)
            val filtered = newItems.filterNot { getSolvedIds().contains(it.puzzle.id) }
            if (filtered.isEmpty()) return@launch

            _puzzles.value = _puzzles.value + filtered
            if (_currentPuzzle.value == null && _puzzles.value.isNotEmpty()) {
                _currentIndex.value = 0
                _currentPuzzle.value = _puzzles.value.first()
            }
        }
    }

    private fun checkForUpdatesInBackground() {
        viewModelScope.launch {
            try {
                _isUpdating.value = true
                _downloadProgress.value = 0f
                _unpackProgress.value = 0f

                val manifestUrl = "https://drive.google.com/uc?export=download&id=18WfPI-rqRNeQbv1BdRU3to919EzYS0cJ"
                val folderName = PuzzleUpdateManager.checkForUpdatesWithPackage(
                    context = context,
                    manifestUrl = manifestUrl,
                    onDownloadProgress = { _downloadProgress.value = it },
                    onUnpackProgress = { _unpackProgress.value = it }
                )
                if (folderName != null) {
                    // пакет обновился — просто добавим пазлы
                    addPackagePuzzles(folderName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isUpdating.value = false
            }
        }
    }

    // ---------------- user actions ----------------

    fun toggleAnswer() {
        val show = !_showAnswer.value
        _showAnswer.value = show
        saveProgress(_currentIndex.value, show)

        if (show) {
            _currentPuzzle.value?.puzzle?.id?.let { addSolvedId(it) }
        }
    }

    fun nextPuzzle() {
        if (_puzzles.value.isEmpty()) return
        removeCurrentIfSolved()
        _currentIndex.value = (_currentIndex.value + 1) % _puzzles.value.size
        _showAnswer.value = false
        saveProgress(_currentIndex.value, false)
        _currentPuzzle.value = _puzzles.value.getOrNull(_currentIndex.value)
    }

    fun prevPuzzle() {
        if (_puzzles.value.isEmpty()) return
        removeCurrentIfSolved()
        _currentIndex.value = (_currentIndex.value - 1 + _puzzles.value.size) % _puzzles.value.size
        _showAnswer.value = false
        saveProgress(_currentIndex.value, false)
        _currentPuzzle.value = _puzzles.value.getOrNull(_currentIndex.value)
    }

    fun resetProgress() {
        saveSolvedIds(emptySet())
        _currentIndex.value = 0
        _showAnswer.value = false
        saveProgress(0, false)
        loadAllLocalAndAssets()
    }

    private fun removeCurrentIfSolved() {
        val curr = _currentPuzzle.value ?: return
        val solved = getSolvedIds()
        if (solved.contains(curr.puzzle.id)) {
            _puzzles.value = _puzzles.value.filter { it.puzzle.id != curr.puzzle.id }
            if (_currentIndex.value >= _puzzles.value.size) {
                _currentIndex.value = (_puzzles.value.size - 1).coerceAtLeast(0)
            }
            _currentPuzzle.value = _puzzles.value.getOrNull(_currentIndex.value)
        }
    }

    private fun saveProgress(index: Int, showAnswer: Boolean) {
        prefs.edit()
            .putInt(KEY_INDEX, index)
            .putBoolean(KEY_SHOW_ANSWER, showAnswer)
            .apply()
    }

    // ---------------- solved storage ----------------

    private fun getSolvedIds(): MutableSet<Int> {
        val raw = prefs.getStringSet(SOLVED_KEY, emptySet()) ?: emptySet()
        return raw.mapNotNull { it.toIntOrNull() }.toMutableSet()
    }

    private fun saveSolvedIds(ids: Set<Int>) {
        prefs.edit().putStringSet(SOLVED_KEY, ids.map { it.toString() }.toSet()).apply()
    }

    private fun addSolvedId(id: Int) {
        val ids = getSolvedIds()
        if (ids.add(id)) saveSolvedIds(ids)
    }
}
