package com.example.savvyclub.viewmodel

import android.app.Application
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.savvyclub.data.PuzzleLoader
import com.example.savvyclub.data.PuzzleUpdateManager
import com.example.savvyclub.data.model.PuzzleItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// ViewModel для приложения SavvyClub, управляет состоянием головоломок
class SavvyClubViewModel(application: Application) : AndroidViewModel(application) {

    // SharedPreferences для хранения текущего прогресса и состояния
    private val prefs = application.getSharedPreferences("puzzle_prefs", Application.MODE_PRIVATE)

    // Ключи для SharedPreferences
    private val keyIndex = "current_index"         // текущий индекс головоломки
    private val keyShowAnswer = "show_answer"     // показывать ли ответ
    private val solvedKey = "solved_ids_set"      // ID решённых головоломок

    // Все головоломки (локальные + ассеты)
    private val _allPuzzles = MutableStateFlow<List<PuzzleItem>>(emptyList())
    val allPuzzles: StateFlow<List<PuzzleItem>> = _allPuzzles.asStateFlow()

    // Отфильтрованные головоломки (по выбранным типам)
    private val _puzzles = MutableStateFlow<List<PuzzleItem>>(emptyList())
    val puzzles: StateFlow<List<PuzzleItem>> = _puzzles.asStateFlow()

    // Индекс текущей головоломки
    private val _currentIndex = MutableStateFlow(prefs.getInt(keyIndex, 0))
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    // Показывать ли ответ
    private val _showAnswer = MutableStateFlow(prefs.getBoolean(keyShowAnswer, false))
    val showAnswer: StateFlow<Boolean> = _showAnswer.asStateFlow()

    // Текущая головоломка
    private val _currentPuzzle = MutableStateFlow<PuzzleItem?>(null)
    val currentPuzzle: StateFlow<PuzzleItem?> = _currentPuzzle.asStateFlow()

    // Выбранные фильтры по типу головоломки
    private val _selectedTypes = MutableStateFlow<Set<String>>(emptySet())
    val selectedTypes: StateFlow<Set<String>> = _selectedTypes.asStateFlow()

    // Флаг, что идет обновление
    private val _isUpdating = MutableStateFlow(false)

    // Прогресс скачивания обновлений
    private val _downloadProgress = MutableStateFlow(0f)

    // Прогресс распаковки обновлений
    private val _unpackProgress = MutableStateFlow(0f)

    // Кэш решённых ID для ускорения работы
    private var solvedCache: MutableSet<Int>? = null

    // --- Firebase ---
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    // --- Состояние профиля ---
    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail

    private val _selectedAvatar = MutableStateFlow("") // "res:12345" или url
    val selectedAvatar: StateFlow<String> = _selectedAvatar

    init {
        // Загружаем все локальные и ассетные головоломки
        loadAllLocalAndAssets()

        // сразу загружаем данные профиля
        loadUserProfile()

        // Проверяем обновления в фоне с задержкой 1 сек
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000) // задержка 1 сек
            checkForUpdatesInBackground()
        }
    }

    /** Сохраняем текущего пользователя в БД (например после логина через Google) */
    fun saveCurrentUserToDb() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val name = user.displayName ?: ""
        val email = user.email ?: ""
        val photoUrl = user.photoUrl?.toString() ?: ""

        val userMap = mapOf(
            "name" to name,
            "email" to email,
            "avatar" to photoUrl // сразу положим google url, если есть
        )

        db.child("users").child(uid).setValue(userMap)
    }

    /** Загружаем имя, email и аватар из БД */
    fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        db.child("users").child(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                _userName.value = snapshot.child("name").getValue(String::class.java) ?: ""
                _userEmail.value = snapshot.child("email").getValue(String::class.java) ?: ""
                _selectedAvatar.value = snapshot.child("avatar").getValue(String::class.java) ?: ""
            }
    }

    /** Сохраняем выбранный аватар в БД */
    private fun saveAvatarToDb(value: String) {
        val uid = auth.currentUser?.uid ?: return
        // users/<uid>/avatar = "res:12345" или URL
        db.child("users")
            .child(uid)
            .child("avatar")
            .setValue(value)
    }

    // Загружает все головоломки с устройства и из ассетов
    private fun loadAllLocalAndAssets() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val solved = getSolvedIds() // Получаем кэш решённых головоломок

            val all = withContext(Dispatchers.IO) {
                PuzzleLoader.loadAllPuzzlesWithSource(context)
                    .filterNot { it.puzzle.id in solved } // исключаем уже решённые
            }

            _allPuzzles.value = all
            applyFilter() // применяем фильтры (если есть)
        }
    }

    // Добавление новых головоломок из пакета обновлений
    private fun addPackagePuzzles(packageFolder: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val solved = getSolvedIds()

            val newItems = withContext(Dispatchers.IO) {
                PuzzleLoader.loadFromPackage(context, packageFolder)
            }

            val filtered = newItems.filterNot { it.puzzle.id in solved }
            if (filtered.isEmpty()) return@launch

            _allPuzzles.value = _allPuzzles.value + filtered
            applyFilter()
        }
    }

    // Проверка обновлений в фоне
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
                    addPackagePuzzles(folderName) // добавляем новые головоломки
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isUpdating.value = false
            }
        }
    }

    // Применение фильтров по типу головоломки
    private fun applyFilter() {
        val filters = _selectedTypes.value
        val allPuzzlesCopy = _allPuzzles.value.toList() // копия списка

        viewModelScope.launch(Dispatchers.Default) {
            val filtered = if (filters.isEmpty()) allPuzzlesCopy
            else allPuzzlesCopy.filter { it.puzzle.type in filters }

            val index = _currentIndex.value
            val newIndex = if (filtered.isEmpty()) 0 else index.coerceAtMost(filtered.lastIndex)
            val current = filtered.getOrNull(newIndex)

            // Обновляем StateFlow на главном потоке
            withContext(Dispatchers.Main) {
                _puzzles.value = filtered
                _currentIndex.value = newIndex
                _currentPuzzle.value = current
            }
        }
    }

    // Включение/выключение фильтра
    fun toggleFilter(type: String) {
        val current = _selectedTypes.value.toMutableSet()
        if (!current.add(type)) current.remove(type)
        _selectedTypes.value = current
        applyFilter()
    }

    // Переключение отображения ответа
    fun toggleAnswer() {
        val current = _currentPuzzle.value ?: return
        val show = !_showAnswer.value
        _showAnswer.value = show
        saveProgress(_currentIndex.value, show) // сохраняем прогресс

        if (show) {
            addSolvedId(current.puzzle.id)
            applyFilter() // обновляем список, исключая решённые
        }
    }

    // Переход к следующей головоломке
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

    // Переход к предыдущей головоломке
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

    // Сброс прогресса всех головоломок
    fun resetProgress() {
        saveSolvedIds(emptySet())
        _currentIndex.value = 0
        _showAnswer.value = false
        saveProgress(0, false)
        loadAllLocalAndAssets()
    }

    // Удаление текущей головоломки, если она решена
    private fun removeCurrentIfSolved() {
        val curr = _currentPuzzle.value ?: return
        val solved = getSolvedIds()
        if (solved.contains(curr.puzzle.id)) {
            _allPuzzles.value = _allPuzzles.value.filter { it.puzzle.id != curr.puzzle.id }
            applyFilter()
        }
    }

    // Сохранение прогресса (индекс + показывать ответ)
    private fun saveProgress(index: Int, showAnswer: Boolean) {
        prefs.edit {
            putInt(keyIndex, index)
            putBoolean(keyShowAnswer, showAnswer)
        }
    }

    // Сброс индекса на 0
    fun resetIndex() {
        _currentIndex.value = 0
        _currentPuzzle.value = _puzzles.value.getOrNull(0)
    }

    // Получение множества ID решённых головоломок
    private fun getSolvedIds(): MutableSet<Int> {
        solvedCache?.let { return it } // если уже есть кэш

        val raw = prefs.getStringSet(solvedKey, emptySet()) ?: emptySet()
        val set = raw.mapNotNull { it.toIntOrNull() }.toMutableSet()
        solvedCache = set
        return set
    }

    // Сохранение множества ID решённых головоломок
    private fun saveSolvedIds(ids: Set<Int>) {
        solvedCache = ids.toMutableSet()
        prefs.edit {
            putStringSet(solvedKey, ids.map { it.toString() }.toSet())
        }
    }

    // Добавление одного решённого ID
    private fun addSolvedId(id: Int) {
        val ids = getSolvedIds()
        if (ids.add(id)) saveSolvedIds(ids)
    }
}
