package com.example.savvyclub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savvyclub.R
import com.example.savvyclub.data.model.Comrade
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ComradesViewModel : ViewModel() {

    private val _comrades = MutableStateFlow<List<Comrade>>(emptyList())
    val comrades: StateFlow<List<Comrade>> = _comrades

    /**
     * Загружаем список товарищей.
     * @param onComplete колбек, который вызывается после завершения загрузки (успех или ошибка)
     */
    fun loadComrades(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val dbRef = FirebaseDatabase.getInstance().getReference("users")
            dbRef.get()
                .addOnSuccessListener { snapshot ->
                    val list = snapshot.children.mapNotNull { child ->
                        val name = child.child("name").getValue(String::class.java) ?: ""
                        val avatar = child.child("avatar").getValue(String::class.java) ?: "res:${R.drawable.default_avatar}"
                        val online = child.child("online").getValue(Boolean::class.java) ?: false

                        Comrade(
                            uid = child.key ?: "",
                            name = if (name.isNotEmpty()) name else "(Без имени)",
                            email = "", // пусто или null
                            avatarUrl = avatar,
                            isOnline = online
                        )
                    }


                    _comrades.value = list
                    onComplete?.invoke()
                }
                .addOnFailureListener {
                    _comrades.value = emptyList()
                    onComplete?.invoke()
                }
        }
    }
}
