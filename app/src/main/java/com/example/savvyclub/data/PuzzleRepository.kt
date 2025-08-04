package com.example.savvyclub.data

import android.content.Context
import com.example.savvyclub.data.model.PuzzleItem

object PuzzleRepository {
    suspend fun loadAll(context: Context): List<PuzzleItem> {
        return PuzzleLoader.loadAllPuzzlesWithSource(context)
    }

}
