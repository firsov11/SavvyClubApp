package com.example.savvyclub.viewmodel

import android.app.Application
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.savvyclub.data.PuzzleLoader
import com.example.savvyclub.data.model.PuzzleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavvyClubViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("puzzle_prefs", Application.MODE_PRIVATE)

    private val keyIndex = "current_index"
    private val keyShowAnswer = "show_answer"
    private val solvedKey = "solved_ids_set"

    private val _allPuzzles = MutableStateFlow<List<PuzzleItem>>(emptyList())
    val allPuzzles: StateFlow<List<PuzzleItem>> = _allPuzzles.asStateFlow()

    private val _puzzles = MutableStateFlow<List<PuzzleItem>>(emptyList())
    val puzzles: StateFlow<List<PuzzleItem>> = _puzzles.asStateFlow()

    private val _currentIndex = MutableStateFlow(prefs.getInt(keyIndex, 0))
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _showAnswer = MutableStateFlow(prefs.getBoolean(keyShowAnswer, false))
    val showAnswer: StateFlow<Boolean> = _showAnswer.asStateFlow()

    private val _currentPuzzle = MutableStateFlow<PuzzleItem?>(null)
    val currentPuzzle: StateFlow<PuzzleItem?> = _currentPuzzle.asStateFlow()

    private val _selectedTypes = MutableStateFlow<Set<String>>(emptySet())
    val selectedTypes: StateFlow<Set<String>> = _selectedTypes.asStateFlow()

    private var solvedCache: MutableSet<Int>? = null

    init {
        loadAllLocalAndAssets()
    }

    /** Загружает все головоломки (из ассетов и локальных пакетов) */
    private fun loadAllLocalAndAssets() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val solved = getSolvedIds()

            val all = withContext(Dispatchers.IO) {
                PuzzleLoader.loadAllPuzzlesWithSource(context)
                    .filterNot { it.puzzle.id in solved }
            }

            _allPuzzles.value = all
            applyFilter()
        }
    }

    /** Применяет фильтр по типам */
    private fun applyFilter() {
        val filters = _selectedTypes.value
        val allPuzzlesCopy = _allPuzzles.value.toList()

        viewModelScope.launch(Dispatchers.Default) {
            val filtered = if (filters.isEmpty()) allPuzzlesCopy
            else allPuzzlesCopy.filter { it.puzzle.type in filters }

            val index = _currentIndex.value
            val newIndex = if (filtered.isEmpty()) 0 else index.coerceAtMost(filtered.lastIndex)
            val current = filtered.getOrNull(newIndex)

            withContext(Dispatchers.Main) {
                _puzzles.value = filtered
                _currentIndex.value = newIndex
                _currentPuzzle.value = current
            }
        }
    }

    /** Включение/выключение фильтра */
    fun toggleFilter(type: String) {
        val current = _selectedTypes.value.toMutableSet()
        if (!current.add(type)) current.remove(type)
        _selectedTypes.value = current
        applyFilter()
    }

    /** Показать/скрыть ответ */
    fun toggleAnswer() {
        val current = _currentPuzzle.value ?: return
        val show = !_showAnswer.value
        _showAnswer.value = show
        saveProgress(_currentIndex.value, show)

        if (show) {
            addSolvedId(current.puzzle.id)
            applyFilter()
        }
    }

    /** Следующая головоломка */
    fun nextPuzzle() {
        val list = _puzzles.value
        if (list.isEmpty()) return

        val curr = _currentPuzzle.value
        if (curr != null && getSolvedIds().contains(curr.puzzle.id)) removeCurrentIfSolved()

        val newList = _puzzles.value
        if (newList.isEmpty()) return

        _currentIndex.value = (_currentIndex.value + 1) % newList.size
        _showAnswer.value = false
        saveProgress(_currentIndex.value, false)
        _currentPuzzle.value = newList[_currentIndex.value]
    }

    /** Предыдущая головоломка */
    fun prevPuzzle() {
        val list = _puzzles.value
        if (list.isEmpty()) return

        val curr = _currentPuzzle.value
        if (curr != null && getSolvedIds().contains(curr.puzzle.id)) removeCurrentIfSolved()

        val newList = _puzzles.value
        if (newList.isEmpty()) return

        _currentIndex.value = (_currentIndex.value - 1 + newList.size) % newList.size
        _showAnswer.value = false
        saveProgress(_currentIndex.value, false)
        _currentPuzzle.value = newList[_currentIndex.value]
    }

    /** Сброс прогресса */
    fun resetProgress() {
        saveSolvedIds(emptySet())
        _currentIndex.value = 0
        _showAnswer.value = false
        saveProgress(0, false)
        loadAllLocalAndAssets()
    }

    /** Удаление решённой головоломки */
    private fun removeCurrentIfSolved() {
        val curr = _currentPuzzle.value ?: return
        val solved = getSolvedIds()
        if (solved.contains(curr.puzzle.id)) {
            _allPuzzles.value = _allPuzzles.value.filter { it.puzzle.id != curr.puzzle.id }
            applyFilter()
        }
    }

    /** Сохранение индекса и состояния показа ответа */
    private fun saveProgress(index: Int, showAnswer: Boolean) {
        prefs.edit {
            putInt(keyIndex, index)
            putBoolean(keyShowAnswer, showAnswer)
        }
    }

    fun resetIndex() {
        _currentIndex.value = 0
        _currentPuzzle.value = _puzzles.value.getOrNull(0)
    }

    /** Работа с решёнными ID */
    private fun getSolvedIds(): MutableSet<Int> {
        solvedCache?.let { return it }
        val raw = prefs.getStringSet(solvedKey, emptySet()) ?: emptySet()
        val set = raw.mapNotNull { it.toIntOrNull() }.toMutableSet()
        solvedCache = set
        return set
    }

    private fun saveSolvedIds(ids: Set<Int>) {
        solvedCache = ids.toMutableSet()
        prefs.edit {
            putStringSet(solvedKey, ids.map { it.toString() }.toSet())
        }
    }

    private fun addSolvedId(id: Int) {
        val ids = getSolvedIds()
        if (ids.add(id)) saveSolvedIds(ids)
    }
}
