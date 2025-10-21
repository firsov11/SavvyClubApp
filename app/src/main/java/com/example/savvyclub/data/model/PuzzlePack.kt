package com.example.savvyclub.data.model

data class PuzzlePack(
    val id: String = "",
    val title: String = "",
    val price: String = "0.00",
    val fileId: String = "",
    val isFree: Boolean = false,
    val isPurchased: Boolean = false,
    val isDownloaded: Boolean = false,
    val md5: String = "" // 🔹 контрольная сумма архива
)


