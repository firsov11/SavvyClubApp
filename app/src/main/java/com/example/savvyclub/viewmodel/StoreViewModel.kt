package com.example.savvyclub.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.savvyclub.data.PuzzleUpdateManager
import com.example.savvyclub.data.model.PuzzlePack
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val app: Application
) : AndroidViewModel(app) {

    private val _packs = MutableStateFlow<List<PuzzlePack>>(emptyList())
    val packs: StateFlow<List<PuzzlePack>> = _packs

    private val dbRef = FirebaseDatabase.getInstance().getReference("packs")
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    init {
        observePacks()
    }

    private fun observePacks() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { snap ->
                    snap.getValue(PuzzlePack::class.java)?.copy(
                        id = snap.key ?: "",
                        isFree = snap.child("isFree").getValue(Boolean::class.java) ?: false
                    )
                }

                _packs.value = list
                syncPurchases()
                autoInstallFreePacks()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("StoreVM", "Firebase cancelled: ${error.message}")
            }
        })
    }

    private fun syncPurchases() {
        val userId = uid ?: return
        val purchaseRef = FirebaseDatabase.getInstance().getReference("purchases/$userId")

        purchaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val purchasedIds = snapshot.children.mapNotNull { it.key }

                val updated = _packs.value.map { pack ->
                    val isBought = pack.isFree || purchasedIds.contains(pack.id)
                    val localDir = File(app.filesDir, "puzzles/${pack.id}")

                    // Для платных пакетов isDownloaded = true только после покупки
                    pack.copy(
                        isPurchased = isBought,
                        isDownloaded = if (pack.isFree) localDir.exists() else isBought && localDir.exists()
                    )
                }

                _packs.value = updated

                viewModelScope.launch(Dispatchers.IO) {
                    // Автоустановка только для платных пакетов, которые куплены и не скачаны
                    updated.filter { it.isPurchased && !it.isDownloaded && !it.isFree }
                        .forEach { autoInstallPack(app, it) }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun autoInstallFreePacks() {
        viewModelScope.launch(Dispatchers.IO) {
            _packs.value.filter { it.isFree }.forEach { pack ->
                val localDir = File(app.filesDir, "puzzles/${pack.id}")
                if (!localDir.exists()) {
                    try {
                        PuzzleUpdateManager.ensurePackageInstalled(
                            context = app,
                            packageId = pack.id,
                            fileId = pack.fileId,
                            expectedHash = pack.md5
                        )
                        _packs.value = _packs.value.map {
                            if (it.id == pack.id) it.copy(isDownloaded = true) else it
                        }
                    } catch (e: Exception) {
                        Log.e("StoreVM", "Failed to auto-install free pack ${pack.id}", e)
                    }
                }
            }
        }
    }

    private suspend fun autoInstallPack(context: Context, pack: PuzzlePack) {
        if (pack.fileId.isBlank() || pack.md5.isBlank()) return

        try {
            val installed = PuzzleUpdateManager.ensurePackageInstalled(
                context = context,
                packageId = pack.id,
                fileId = pack.fileId,
                expectedHash = pack.md5
            )

            if (installed) {
                _packs.value = _packs.value.map {
                    if (it.id == pack.id) it.copy(isDownloaded = true) else it
                }
            }
        } catch (e: Exception) {
            Log.e("StoreVM", "Failed to auto-install ${pack.id}: ${e.message}", e)
        }
    }

    fun downloadPack(context: Context, pack: PuzzlePack) {
        viewModelScope.launch(Dispatchers.IO) {
            autoInstallPack(context, pack)
        }
    }

    fun buyPack(pack: PuzzlePack) {
        // TODO: интеграция покупки
        Log.i("StoreVM", "buyPack called for ${pack.id}")
    }
}

