package com.example.savvyclub.data.model

data class Comrade(
    val uid: String? = null,
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = "", // может быть url или "res:123"
    val isOnline: Boolean = false
) {
    /** Готовая мапа для Firebase */
    fun toMap(): Map<String, Any?> = mapOf(
        "name" to name,
        "email" to email,
        "avatar" to avatarUrl,
        "online" to isOnline
    )
}
