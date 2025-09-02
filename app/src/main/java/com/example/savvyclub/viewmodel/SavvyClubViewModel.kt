package com.example.savvyclub.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.savvyclub.data.PuzzleLoader
import com.example.savvyclub.data.PuzzleUpdateManager
import com.example.savvyclub.data.model.PuzzleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavvyClubViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("puzzle_prefs", Application.MODE_PRIVATE)

    private val KEY_INDEX = "current_index"
    private val KEY_SHOW_ANSWER = "show_answer"
    private val SOLVED_KEY = "solved_ids_set"

    private val _allPuzzles = MutableStateFlow<List<PuzzleItem>>(emptyList())
    val allPuzzles: StateFlow<List<PuzzleItem>> = _allPuzzles.asStateFlow()

    private val _puzzles = MutableStateFlow<List<PuzzleItem>>(emptyList())
    val puzzles: StateFlow<List<PuzzleItem>> = _puzzles.asStateFlow()

    private val _currentIndex = MutableStateFlow(prefs.getInt(KEY_INDEX, 0))
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _showAnswer = MutableStateFlow(prefs.getBoolean(KEY_SHOW_ANSWER, false))
    val showAnswer: StateFlow<Boolean> = _showAnswer.asStateFlow()

    private val _currentPuzzle = MutableStateFlow<PuzzleItem?>(null)
    val currentPuzzle: StateFlow<PuzzleItem?> = _currentPuzzle.asStateFlow()

    private val _selectedTypes = MutableStateFlow<Set<String>>(emptySet())
    val selectedTypes: StateFlow<Set<String>> = _selectedTypes.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _unpackProgress = MutableStateFlow(0f)
    val unpackProgress: StateFlow<Float> = _unpackProgress.asStateFlow()

    private var solvedCache: MutableSet<Int>? = null

    init {
        loadAllLocalAndAssets()
        checkForUpdatesInBackground()
    }

    private fun loadAllLocalAndAssets() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val solved = getSolvedIds() // кэшируем один раз

            val all = withContext(Dispatchers.IO) {
                PuzzleLoader.loadAllPuzzlesWithSource(context)
                    .filterNot { it.puzzle.id in solved }
            }

            _allPuzzles.value = all
            applyFilter()
        }
    }

    private fun addPackagePuzzles(packageFolder: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val solved = getSolvedIds() // кэшируем один раз

            val newItems = withContext(Dispatchers.IO) {
                PuzzleLoader.loadFromPackage(context, packageFolder)
            }

            val filtered = newItems.filterNot { it.puzzle.id in solved }
            if (filtered.isEmpty()) return@launch

            _allPuzzles.value = _allPuzzles.value + filtered
            applyFilter()
        }
    }

    private fun checkForUpdatesInBackground() {
        viewModelScope.launch {
            try {
                _isUpdating.value = true
                _downloadProgress.value = 0f
                _unpackProgress.value = 0f

                val context = getApplication<Application>()
                val manifestUrl = "https://drive.google.com/uc?export=download&id=18WfPI-rqRNeQbv1BdRU3to919EzYS0cJ"

                val folderName = withContext(Dispatchers.IO) {
                    PuzzleUpdateManager.checkForUpdatesWithPackage(
                        context = context,
                        manifestUrl = manifestUrl,
                        onDownloadProgress = { _downloadProgress.value = it },
                        onUnpackProgress = { _unpackProgress.value = it }
                    )
                }

                if (folderName != null) {
                    addPackagePuzzles(folderName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isUpdating.value = false
            }
        }
    }

    private fun applyFilter() {
        val filters = _selectedTypes.value
        val allPuzzlesCopy = _allPuzzles.value.toList() // чтобы работать с копией

        viewModelScope.launch(Dispatchers.Default) {
            val filtered = if (filters.isEmpty()) allPuzzlesCopy
            else allPuzzlesCopy.filter { it.puzzle.type in filters }

            val index = _currentIndex.value
            val newIndex = if (filtered.isEmpty()) 0 else index.coerceAtMost(filtered.lastIndex)
            val current = filtered.getOrNull(newIndex)

            // Возвращаем результат на главный поток
            withContext(Dispatchers.Main) {
                _puzzles.value = filtered
                _currentIndex.value = newIndex
                _currentPuzzle.value = current
            }
        }
    }


    fun toggleFilter(type: String) {
        val current = _selectedTypes.value.toMutableSet()
        if (!current.add(type)) current.remove(type)
        _selectedTypes.value = current
        applyFilter()
    }

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
            _allPuzzles.value = _allPuzzles.value.filter { it.puzzle.id != curr.puzzle.id }
            applyFilter()
        }
    }

    private fun saveProgress(index: Int, showAnswer: Boolean) {
        prefs.edit()
            .putInt(KEY_INDEX, index)
            .putBoolean(KEY_SHOW_ANSWER, showAnswer)
            .apply()
    }

    fun resetIndex() {
        _currentIndex.value = 0
        _currentPuzzle.value = _puzzles.value.getOrNull(0)
    }

    private fun getSolvedIds(): MutableSet<Int> {
        solvedCache?.let { return it }

        val raw = prefs.getStringSet(SOLVED_KEY, emptySet()) ?: emptySet()
        val set = raw.mapNotNull { it.toIntOrNull() }.toMutableSet()
        solvedCache = set
        return set
    }

    private fun saveSolvedIds(ids: Set<Int>) {
        solvedCache = ids.toMutableSet()
        prefs.edit().putStringSet(SOLVED_KEY, ids.map { it.toString() }.toSet()).apply()
    }

    private fun addSolvedId(id: Int) {
        val ids = getSolvedIds()
        if (ids.add(id)) saveSolvedIds(ids)
    }
}
